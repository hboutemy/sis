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
package org.apache.sis.internal.coveragejson;

import jakarta.json.bind.JsonbBuilder;
import java.awt.image.Raster;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.eclipse.yasson.internal.JsonBindingBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.metadata.spatial.DimensionNameType;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageJsonStoreTest extends TestCase {

    /**
     * Test coverage example from https://covjson.org/playground/.
     */
    @Test
    public void testCoverageXYZT() throws Exception {

        try (final DataStore store = new CoverageJsonStoreProvider().open(new StorageConnector(CoverageJsonStoreTest.class.getResource("coverage_xyzt.json")))) {

            //test grid coverage resource exist
            Assert.assertTrue(store instanceof Aggregate);
            final Aggregate aggregate = (Aggregate) store;
            Assert.assertEquals(1, aggregate.components().size());
            final Resource candidate = aggregate.components().iterator().next();
            Assert.assertTrue(candidate instanceof GridCoverageResource);
            final GridCoverageResource gcr = (GridCoverageResource) candidate;

            JsonbBuilder jcb = new JsonBindingBuilder();
            { //test grid geometry
                final GridGeometry result = gcr.getGridGeometry();
                System.out.println(result);

                Assert.assertEquals(4, result.getDimension());

                final GridExtent expectedExtent = new GridExtent(new DimensionNameType[]{
                    DimensionNameType.valueOf("x"),
                    DimensionNameType.valueOf("y"),
                    DimensionNameType.valueOf("z"),
                    DimensionNameType.valueOf("t")},
                        new long[]{0,0,0,0}, new long[]{2,1,0,0}, true);
                Assert.assertEquals(expectedExtent, result.getExtent());
                Assert.assertEquals(CRS.compound(CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.JAVA.crs()), result.getCoordinateReferenceSystem());
                //TODO test transform
            }


            {   //test data
                GridCoverage coverage = gcr.read(null);
                Raster data = coverage.render(null).getData();
            }
        }

    }

}
