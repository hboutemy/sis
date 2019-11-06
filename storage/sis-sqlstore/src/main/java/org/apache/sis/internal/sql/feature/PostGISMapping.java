package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.logging.Logging;

public final class PostGISMapping implements DialectMapping {

    final PostGISMapping.Spi spi;
    final GeometryIdentification identifyGeometries;
    final GeometryIdentification identifyGeographies;

    final Connection connection;

    final Geometries library;

    /**
     * A cache valid ONLY FOR A DATASOURCE. IT'S IMPORTANT ! Why ? Because :
     * <ul>
     *     <li>CRS definition could differ between databases (PostGIS version, user alterations, etc.)</li>
     *     <li>Avoid inter-database locking</li>
     * </ul>
     */
    final Cache<Integer, CoordinateReferenceSystem> sessionCache;

    private PostGISMapping(final PostGISMapping.Spi spi, Connection c) throws SQLException {
        connection = c;
        this.spi = spi;
        sessionCache = new Cache<>(7, 0, true);
        this.identifyGeometries = new GeometryIdentification(c, "geometry_columns", "f_geometry_column", "type", sessionCache);
        this.identifyGeographies = new GeometryIdentification(c, "geography_columns", "f_geography_column", "type", sessionCache);

        this.library = Geometries.implementation(null);
    }

    @Override
    public Spi getSpi() {
        return spi;
    }

    @Override
    public Optional<ColumnAdapter<?>> getMapping(SQLColumn definition) {
        switch (definition.type) {
            case (Types.OTHER): return Optional.ofNullable(forOther(definition));
        }
        return Optional.empty();
    }

    private ColumnAdapter<?> forOther(SQLColumn definition) {
        switch (definition.typeName.trim().toLowerCase()) {
            case "geometry":
                return forGeometry(definition, identifyGeometries);
            case "geography":
                return forGeometry(definition, identifyGeographies);
            default: return null;
        }
    }

    private ColumnAdapter<?> forGeometry(SQLColumn definition, GeometryIdentification ident) {
        // In case of a computed column, geometric definition could be null.
        final GeometryIdentification.GeometryColumn geomDef;
        try {
            geomDef = ident.fetch(definition).orElse(null);
        } catch (SQLException | ParseException e) {
            throw new BackingStoreException(e);
        }
        String geometryType = geomDef == null ? null : geomDef.type;
        final Class geomClass = getGeometricClass(geometryType);

        if (geomDef == null || geomDef.crs == null) {
            return new HexEWKBDynamicCrs(geomClass);
        } else {
            // TODO: activate optimisation : WKB is lighter, but we need to modify user query, and to know CRS in advance.
            //geometryDecoder = new WKBReader(geomDef.crs);
            return new HexEWKBFixedCrs(geomClass, geomDef.crs);
        }
    }

    private Class getGeometricClass(String geometryType) {
        if (geometryType == null) return library.rootClass;

        // remove Z, M or ZM suffix
        if (geometryType.endsWith("M")) geometryType = geometryType.substring(0, geometryType.length()-1);
        if (geometryType.endsWith("Z")) geometryType = geometryType.substring(0, geometryType.length()-1);

        final Class geomClass;
        switch (geometryType) {
            case "POINT":
                geomClass = library.pointClass;
                break;
            case "LINESTRING":
                geomClass = library.polylineClass;
                break;
            case "POLYGON":
                geomClass = library.polygonClass;
                break;
            default: geomClass = library.rootClass;
        }
        return geomClass;
    }

    @Override
    public void close() throws SQLException {
        identifyGeometries.close();
    }

    public static final class Spi implements DialectMapping.Spi {

        @Override
        public Optional<DialectMapping> create(Connection c) throws SQLException {
            try {
                checkPostGISVersion(c);
            } catch (SQLException e) {
                final Logger logger = Logging.getLogger("org.apache.sis.internal.sql");
                logger.warning("No compatible PostGIS version found. Binding deactivated. See debug logs for more information");
                logger.log(Level.FINE, "Cannot determine PostGIS version", e);
                return Optional.empty();
            }
            return Optional.of(new PostGISMapping(this, c));
        }

        private void checkPostGISVersion(final Connection c) throws SQLException {
            try (
                    Statement st = c.createStatement();
                    ResultSet result = st.executeQuery("SELECT PostGIS_version();");
            ) {
                result.next();
                final String pgisVersion = result.getString(1);
                if (!pgisVersion.startsWith("2.")) throw new SQLException("Incompatible PostGIS version. Only 2.x is supported for now, but database declares: ");
            }
        }

        @Override
        public Dialect getDialect() {
            return Dialect.POSTGRESQL;
        }
    }

    private abstract class Reader implements ColumnAdapter {

        final Class geomClass;

        public Reader(Class geomClass) {
            this.geomClass = geomClass;
        }

        @Override
        public Class getJavaType() {
            return geomClass;
        }
    }

    private final class WKBReader extends Reader implements SQLBiFunction<ResultSet, Integer, Object> {

        final CoordinateReferenceSystem crsToApply;

        private WKBReader(Class geomClass, CoordinateReferenceSystem crsToApply) {
            super(geomClass);
            this.crsToApply = crsToApply;
        }

        @Override
        public Object apply(ResultSet resultSet, Integer integer) throws SQLException {
            final byte[] bytes = resultSet.getBytes(integer);
            if (bytes == null) return null;
            final Object value = library.parseWKB(bytes);
            if (value != null && crsToApply != null) {
                library.setCRS(value, crsToApply);
            }

            return value;
        }

        @Override
        public SQLBiFunction prepare(Connection target) {
            return this;
        }

        @Override
        public Optional<CoordinateReferenceSystem> getCrs() {
            return Optional.ofNullable(crsToApply);
        }
    }

    private final class HexEWKBFixedCrs extends Reader {
        final CoordinateReferenceSystem crsToApply;

        public HexEWKBFixedCrs(Class geomClass, CoordinateReferenceSystem crsToApply) {
            super(geomClass);
            this.crsToApply = crsToApply;
        }

        @Override
        public SQLBiFunction prepare(Connection target) {
            return new HexEWKBReader(new EWKBReader(library).forCrs(crsToApply));
        }

        @Override
        public Optional<CoordinateReferenceSystem> getCrs() {
            return Optional.ofNullable(crsToApply);
        }
    }

    private final class HexEWKBDynamicCrs extends Reader {

        public HexEWKBDynamicCrs(Class geomClass) {
            super(geomClass);
        }

        @Override
        public SQLBiFunction prepare(Connection target) {
            // TODO: this component is not properly closed. As connection closing should also close this component
            // statement, it should be Ok.However, a proper management would be better.
            final CRSIdentification crsIdent;
            try {
                crsIdent = new CRSIdentification(target, sessionCache);
            } catch (SQLException e) {
                throw new BackingStoreException(e);
            }
            return new HexEWKBReader(
                    new EWKBReader(library)
                            .withResolver(crsIdent::fetchCrs)
            );
        }
    }

    private static final class HexEWKBReader implements SQLBiFunction<ResultSet, Integer, Object> {

        final EWKBReader reader;

        private HexEWKBReader(EWKBReader reader) {
            this.reader = reader;
        }

        @Override
        public Object apply(ResultSet resultSet, Integer integer) throws SQLException {
            final String hexa = resultSet.getString(integer);
            return hexa == null ? null : reader.readHexa(hexa);
        }
    }
}
