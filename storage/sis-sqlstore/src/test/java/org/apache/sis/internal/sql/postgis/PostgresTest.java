/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.sql.postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.sql.SQLStoreTest;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.sql.feature.BinaryEncoding;
import org.apache.sis.internal.sql.feature.GeometryGetterTest;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.Version;
import org.junit.Test;

// Branch-dependent imports
import org.opengis.feature.Feature;

// Optional dependencies
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Geometry;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link Postgres}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 */
@DependsOn({RasterReaderTest.class, RasterWriterTest.class})
public final class PostgresTest extends TestCase {
    /**
     * Tests {@link Postgres#parseVersion(String)}.
     */
    @Test
    public void testParseVersion() {
        final Version version = Postgres.parseVersion("3.1 USE_GEOS=1 USE_PROJ=1 USE_STATS=1");
        assertEquals(3, version.getMajor());
        assertEquals(1, version.getMinor());
        assertNull  (   version.getRevision());
    }

    /**
     * Tests reading and writing features and rasters.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testSpatialFeatures() throws Exception {
        try (TestDatabase database = TestDatabase.createOnPostgreSQL(SQLStoreTest.SCHEMA, true)) {
            database.executeSQL(PostgresTest.class, "file:SpatialFeatures.sql");
            final StorageConnector connector = new StorageConnector(database.source);
            connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.JTS);
            final ResourceDefinition table = ResourceDefinition.table(null, SQLStoreTest.SCHEMA, "SpatialData");
            try (SQLStore store = new SQLStore(new SQLStoreProvider(), connector, table)) {
                /*
                 * Invoke the private `model()` method. We have to use reflection because the class
                 * is not in the same package and we do not want to expose the method in public API.
                 */
                final Method modelAccessor = SQLStore.class.getDeclaredMethod("model");
                modelAccessor.setAccessible(true);
                final Postgres<?> pg = (Postgres<?>) modelAccessor.invoke(store);
                try (Connection connection = database.source.getConnection();
                     ExtendedInfo info = new ExtendedInfo(pg, connection))
                {
                    testInfoStatements(info);
                    testGeometryGetter(info, connection);
                    testRasterReader(TestRaster.USHORT, info, connection);
                }
                /*
                 * Tests through public API.
                 */
                final FeatureSet resource = store.findResource("SpatialData");
                try (Stream<Feature> features = resource.features(false)) {
                    features.forEach(PostgresTest::validate);
                }
                final Envelope envelope = resource.getEnvelope().get();
                assertEquals(envelope.getMinimum(0), -72, 1);
                assertEquals(envelope.getMaximum(1),  43, 1);
            }
        }
    }

    /**
     * Tests {@link org.apache.sis.internal.sql.feature.InfoStatements}.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    private static void testInfoStatements(final ExtendedInfo info) throws Exception {
        assertEquals("findSRID", 4326, info.findSRID(HardCodedCRS.WGS84));
        final CoordinateReferenceSystem expected = GeometryGetterTest.getExpectedCRS(3395);
        final CoordinateReferenceSystem actual   = info.fetchCRS(3395);
        assertInstanceOf("findSRID", ProjectedCRS.class, actual);
        if (expected != null) {
            assertSame("fetchCRS", expected, actual);
        }
    }

    /**
     * Tests {@link org.apache.sis.internal.sql.feature.GeometryGetter}
     * in the context of querying a database.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    private static void testGeometryGetter(final ExtendedInfo info, final Connection connection) throws Exception {
        final GeometryGetterTest test = new GeometryGetterTest();
        test.testFromDatabase(connection, info, BinaryEncoding.HEXADECIMAL);
    }

    /**
     * Tests {@link RasterReader} in the context of a database.
     */
    private static void testRasterReader(final TestRaster test, final ExtendedInfo info, final Connection connection)
            throws Exception
    {
        final BinaryEncoding encoding = BinaryEncoding.HEXADECIMAL;
        final RasterReader reader = new RasterReader(info);
        try (PreparedStatement stmt = connection.prepareStatement("SELECT image FROM features.\"SpatialData\" WHERE filename=?")) {
            stmt.setString(1, test.filename);
            final ResultSet r = stmt.executeQuery();
            assertTrue(r.next());
            final ReadableByteChannel channel = Channels.newChannel(encoding.decode(r.getBinaryStream(1)));
            final ChannelDataInput input = new ChannelDataInput(test.filename, channel, ByteBuffer.allocate(50), false);
            RasterReaderTest.compareReadResult(test, reader, input);
            assertFalse(r.next());
        }
    }

    /**
     * Invoked for each feature instances for performing some checks on the feature.
     * This method performs only a superficial verification of geometries.
     */
    private static void validate(final Feature feature) {
        final String       filename = feature.getPropertyValue("filename").toString();
        final Geometry     geometry = (Geometry) feature.getPropertyValue("geometry");
        final GridCoverage raster   = (GridCoverage) feature.getPropertyValue("image");
        final int geomSRID;
        switch (filename) {
            case "raster-ushort.wkb": {
                assertNull(geometry);
                RasterReaderTest.compareReadResult(TestRaster.USHORT, raster);
                assertSame(CommonCRS.WGS84.normalizedGeographic(), raster.getCoordinateReferenceSystem());
                return;
            }
            case "point-prj": {
                final Point p = (Point) geometry;
                assertEquals(2, p.getX(), STRICT);
                assertEquals(3, p.getY(), STRICT);
                geomSRID = 3395;
                break;
            }
            case "polygon-prj": geomSRID = 3395; break;
            case "linestring":
            case "polygon":
            case "multi-linestring":
            case "multi-polygon": geomSRID = 4326; break;
            default: throw new AssertionError(filename);
        }
        try {
            final CoordinateReferenceSystem expected = GeometryGetterTest.getExpectedCRS(geomSRID);
            final CoordinateReferenceSystem actual = JTS.getCoordinateReferenceSystem(geometry);
            assertNotNull(actual);
            if (expected != null) {
                assertEquals(expected, actual);
            }
        } catch (FactoryException e) {
            throw new AssertionError(e);
        }
        assertNull(raster);
    }
}
