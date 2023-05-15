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
package org.apache.sis.internal.feature.jts;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.filter.sqlmm.SQLMM;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryType;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;

// Optional dependencies
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

// Branch-dependent imports
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.DistanceOperatorName;


/**
 * The wrapper of Java Topology Suite (JTS) geometries.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.2
 * @since   1.1
 */
final class Wrapper extends GeometryWrapper<Geometry> {
    /**
     * The wrapped implementation.
     */
    private final Geometry geometry;

    /**
     * Creates a new wrapper around the given geometry.
     */
    Wrapper(final Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the given geometry in new wrapper,
     * or {@code this} if {@code g} is same as current geometry.
     *
     * @param  result  the geometry computed by a JTS operation.
     * @return wrapper for the given geometry. May be {@code this}.
     */
    private Wrapper rewrap(final Geometry result) {
        return (result != geometry) ? new Wrapper(result) : this;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    public Geometries<Geometry> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    public Object implementation() {
        return geometry;
    }

    /**
     * Returns the Spatial Reference System Identifier (SRID) if available.
     * This is <em>not</em> necessarily an EPSG code, even it is common practice to use
     * the same numerical values than EPSG. Note that the absence of SRID does not mean
     * that {@link #getCoordinateReferenceSystem()} would return no CRS.
     */
    @Override
    public OptionalInt getSRID() {
        final int srid = geometry.getSRID();
        return (srid != 0) ? OptionalInt.of(srid) : OptionalInt.empty();
    }

    /**
     * Returns the geometry coordinate reference system, or {@code null} if none.
     *
     * @return the coordinate reference system, or {@code null} if none.
     * @throws BackingStoreException if the CRS cannot be created from the SRID code.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        try {
            return JTS.getCoordinateReferenceSystem(geometry);
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Sets the coordinate reference system. This method overwrites any previous user object.
     * This is okay for the context in which Apache SIS uses this method, which is only for
     * newly created geometries.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureDimensionMatches("crs", getCoordinatesDimension(geometry), crs);
        JTS.setCoordinateReferenceSystem(geometry, crs);
    }

    /**
     * Gets the number of dimensions of geometry vertex (sequence of coordinate tuples), which can be 2 or 3.
     * Note that this is different than the {@linkplain Geometry#getDimension() geometry topological dimension},
     * which can be 0, 1 or 2.
     *
     * @param  geometry  the geometry for which to get <em>vertex</em> (not topological) dimension.
     * @return vertex dimension of the given geometry.
     * @throws IllegalArgumentException if the type of the given geometry is not recognized.
     */
    private static int getCoordinatesDimension(final Geometry geometry) {
        final CoordinateSequence cs;
        if (geometry instanceof Point) {
            // Most efficient method (no allocation) in JTS 1.18.
            cs = ((Point) geometry).getCoordinateSequence();
        } else if (geometry instanceof LineString) {
            // Most efficient method (no allocation) in JTS 1.18.
            cs = ((LineString) geometry).getCoordinateSequence();
        } else if (geometry instanceof Polygon) {
            return getCoordinatesDimension(((Polygon) geometry).getExteriorRing());
        } else if (geometry instanceof GeometryCollection) {
            final GeometryCollection gc = (GeometryCollection) geometry;
            final int n = gc.getNumGeometries();
            if (n == 0) {
                return Factory.TRIDIMENSIONAL;      // Undefined coordinates, JTS assumes 3 for empty geometries.
            }
            for (int i=0; i<n; i++) {
                // If at least one geometry is 3D, consider the whole geometry as 3D.
                final int d = getCoordinatesDimension(gc.getGeometryN(i));
                if (d > Factory.BIDIMENSIONAL) return d;
            }
            return Factory.BIDIMENSIONAL;
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, geometry.getGeometryType()));
        }
        return cs.getDimension();
    }

    /**
     * Returns the envelope of the wrapped JTS geometry. Never null, but may be empty.
     * In current implementation, <var>z</var> values of three-dimensional envelopes
     * are {@link Double#NaN}. It may change in a future version if we have a way to
     * get those <var>z</var> values from a JTS object.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final Envelope bounds = geometry.getEnvelopeInternal();
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        final GeneralEnvelope env;
        if (crs != null) {
            env = new GeneralEnvelope(crs);
            env.setToNaN();
        } else {
            env = new GeneralEnvelope(Factory.BIDIMENSIONAL);
        }
        env.setRange(0, bounds.getMinX(), bounds.getMaxX());
        env.setRange(1, bounds.getMinY(), bounds.getMaxY());
        return env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final Coordinate c = geometry.getCentroid().getCoordinate();
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        if (crs == null) {
            final double z = c.getZ();
            if (!Double.isNaN(z)) {
                return new GeneralDirectPosition(c.x, c.y, z);
            }
        } else if (ReferencingUtilities.getDimension(crs) != Factory.BIDIMENSIONAL) {
            final GeneralDirectPosition point = new GeneralDirectPosition(crs);
            point.setOrdinate(0, c.x);
            point.setOrdinate(1, c.y);
            point.setOrdinate(2, c.getZ());
            return point;
        }
        return new DirectPosition2D(crs, c.x, c.y);
    }

    /**
     * If the wrapped geometry is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    public double[] getPointCoordinates() {
        if (!(geometry instanceof Point)) {
            return null;
        }
        final Coordinate pt = ((Point) geometry).getCoordinate();
        final double z = pt.getZ();
        final double[] coord;
        if (Double.isNaN(z)) {
            coord = new double[Factory.BIDIMENSIONAL];
        } else {
            coord = new double[Factory.TRIDIMENSIONAL];
            coord[2] = z;
        }
        coord[1] = pt.y;
        coord[0] = pt.x;
        return coord;
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        final Coordinate[] points = geometry.getCoordinates();
        final double[] coordinates = new double[points.length * Factory.BIDIMENSIONAL];
        int i = 0;
        for (final Coordinate p : points) {
            coordinates[i++] = p.x;
            coordinates[i++] = p.y;
        }
        return coordinates;
    }

    /**
     * Merges a sequence of points or paths after the wrapped geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    public Geometry mergePolylines(final Iterator<?> polylines) {
        final List<Coordinate> coordinates = new ArrayList<>();
        final List<Geometry> lines = new ArrayList<>();
        boolean isFloat = true;
add:    for (Geometry next = geometry;;) {
            if (next instanceof Point) {
                final Coordinate pt = ((Point) next).getCoordinate();
                if (!Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                    isFloat = Factory.isFloat(isFloat, (Point) next);
                    coordinates.add(pt);
                } else {
                    Factory.INSTANCE.toLineString(coordinates, lines, false, isFloat);
                    coordinates.clear();
                    isFloat = true;
                }
            } else {
                final int n = next.getNumGeometries();
                for (int i=0; i<n; i++) {
                    final LineString ls = (LineString) next.getGeometryN(i);
                    if (coordinates.isEmpty()) {
                        lines.add(ls);
                    } else {
                        if (isFloat) isFloat = Factory.isFloat(ls.getCoordinateSequence());
                        coordinates.addAll(Arrays.asList(ls.getCoordinates()));
                        Factory.INSTANCE.toLineString(coordinates, lines, false, isFloat);
                        coordinates.clear();
                        isFloat = true;
                    }
                }
            }
            /*
             * `polylines.hasNext()` check is conceptually part of `for` instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = (Geometry) polylines.next()) == null);
        }
        Factory.INSTANCE.toLineString(coordinates, lines, false, isFloat);
        return Factory.INSTANCE.toGeometry(lines, false, isFloat);
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     *
     * <p><b>Note:</b> {@link SpatialOperatorName#BBOX} is implemented by {@code NOT DISJOINT}.
     * It is caller's responsibility to ensure that one of the geometries is rectangular,
     * for example by a call to {@link Geometry#getEnvelope()}.</p>
     */
    @Override
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper<Geometry> other) {
        final int ordinal = type.ordinal();
        if (ordinal >= 0 && ordinal < PREDICATES.length) {
            final BiPredicate<Geometry,Geometry> op = PREDICATES[ordinal];
            if (op != null) {
                return op.test(geometry, ((Wrapper) other).geometry);
            }
        }
        return super.predicateSameCRS(type, other);
    }

    /**
     * All predicates recognized by {@link #predicate(SpatialOperatorName, Geometry)}.
     * Array indices are {@link SpatialOperatorName#ordinal()} values.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static final BiPredicate<Geometry,Geometry>[] PREDICATES =
            new BiPredicate[SpatialOperatorName.OVERLAPS.ordinal() + 1];
    static {
        PREDICATES[SpatialOperatorName.BBOX      .ordinal()] = (a,b) -> !a.disjoint(b);
        PREDICATES[SpatialOperatorName.EQUALS    .ordinal()] = Geometry::equalsTopo;
        PREDICATES[SpatialOperatorName.DISJOINT  .ordinal()] = Geometry::disjoint;
        PREDICATES[SpatialOperatorName.INTERSECTS.ordinal()] = Geometry::intersects;
        PREDICATES[SpatialOperatorName.TOUCHES   .ordinal()] = Geometry::touches;
        PREDICATES[SpatialOperatorName.CROSSES   .ordinal()] = Geometry::crosses;
        PREDICATES[SpatialOperatorName.WITHIN    .ordinal()] = Geometry::within;
        PREDICATES[SpatialOperatorName.CONTAINS  .ordinal()] = Geometry::contains;
        PREDICATES[SpatialOperatorName.OVERLAPS  .ordinal()] = Geometry::overlaps;
    }

    /**
     * Applies a filter predicate between this geometry and another geometry within a given distance.
     * This method assumes that the two geometries are in the same CRS and that the unit of measurement
     * is the same for {@code distance} than for axes (this is not verified).
     */
    @Override
    protected boolean predicateSameCRS(final DistanceOperatorName type,
            final GeometryWrapper<Geometry> other, final double distance)
    {
        boolean reverse = (type != DistanceOperatorName.WITHIN);
        if (reverse && type != DistanceOperatorName.BEYOND) {
            return super.predicateSameCRS(type, other, distance);
        }
        return geometry.isWithinDistance(((Wrapper) other).geometry, distance) ^ reverse;
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     * @throws ClassCastException if the operation can only be executed on some specific argument types
     *         (for example geometries that are polylines) and one of the argument is not of that type.
     */
    @Override
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper<Geometry> other, final Object argument) {
        /*
         * For all operation producing a geometry, the result is collected for post-processing.
         * For all other kinds of value, the result is returned directly in the switch statement.
         */
        final Geometry result;
        switch (operation) {
            case ST_IsMeasured:       return Boolean.FALSE;
            case ST_Dimension:        return geometry.getDimension();
            case ST_SRID:             return geometry.getSRID();
            case ST_IsEmpty:          return geometry.isEmpty();
            case ST_IsSimple:         return geometry.isSimple();
            case ST_IsValid:          return geometry.isValid();
            case ST_Envelope:         return getEnvelope();
            case ST_Boundary:         result = geometry.getBoundary(); break;
            case ST_ConvexHull:       result = geometry.convexHull(); break;
            case ST_Buffer:           result = geometry.buffer(((Number) argument).doubleValue()); break;
            case ST_Intersection:     result = geometry.intersection (((Wrapper) other).geometry); break;
            case ST_Union:            result = geometry.union        (((Wrapper) other).geometry); break;
            case ST_Difference:       result = geometry.difference   (((Wrapper) other).geometry); break;
            case ST_SymDifference:    result = geometry.symDifference(((Wrapper) other).geometry); break;
            case ST_Distance:         return   geometry.distance     (((Wrapper) other).geometry);
            case ST_Equals:           return   geometry.equalsTopo   (((Wrapper) other).geometry);
            case ST_Relate:           return   geometry.relate       (((Wrapper) other).geometry, argument.toString());
            case ST_Disjoint:         return   geometry.disjoint     (((Wrapper) other).geometry);
            case ST_Intersects:       return   geometry.intersects   (((Wrapper) other).geometry);
            case ST_Touches:          return   geometry.touches      (((Wrapper) other).geometry);
            case ST_Crosses:          return   geometry.crosses      (((Wrapper) other).geometry);
            case ST_Within:           return   geometry.within       (((Wrapper) other).geometry);
            case ST_Contains:         return   geometry.contains     (((Wrapper) other).geometry);
            case ST_Overlaps:         return   geometry.overlaps     (((Wrapper) other).geometry);
            case ST_AsText:           return new WKTWriter().write(geometry);   // WKTWriter() constructor is cheap.
            case ST_AsBinary:         return FilteringContext.writeWKB(geometry);
            case ST_X:                return ((Point) geometry).getX();
            case ST_Y:                return ((Point) geometry).getY();
            case ST_Z:                return ((Point) geometry).getCoordinate().getZ();
            case ST_ToLineString:     return geometry;                          // JTS does not have curves.
            case ST_NumGeometries:    return geometry.getNumGeometries();
            case ST_NumPoints:        return geometry.getNumPoints();
            case ST_PointN:           result = ((LineString) geometry).getPointN(toIndex(argument)); break;
            case ST_StartPoint:       result = ((LineString) geometry).getStartPoint(); break;
            case ST_EndPoint:         result = ((LineString) geometry).getEndPoint(); break;
            case ST_IsClosed:         return   ((LineString) geometry).isClosed();
            case ST_IsRing:           return   ((LineString) geometry).isRing();
            case ST_Perimeter:        // Fallthrough: length is the perimeter for polygons.
            case ST_Length:           return   geometry.getLength();
            case ST_Area:             return   geometry.getArea();
            case ST_Centroid:         result = geometry.getCentroid(); break;
            case ST_PointOnSurface:   result = geometry.getInteriorPoint(); break;
            case ST_ExteriorRing:     result = ((Polygon) geometry).getExteriorRing(); break;
            case ST_InteriorRingN:    result = ((Polygon) geometry).getInteriorRingN(toIndex(argument)); break;
            case ST_NumInteriorRings: return   ((Polygon) geometry).getNumInteriorRing();
            case ST_GeometryN:        result = geometry.getGeometryN(toIndex(argument)); break;
            case ST_ToPoint:
            case ST_ToPolygon:
            case ST_ToMultiPoint:
            case ST_ToMultiLine:
            case ST_ToMultiPolygon:
            case ST_ToGeomColl: {
                final GeometryType target = operation.getGeometryType().get();
                final Class<?> type = factory().getGeometryClass(target);
                if (type.isInstance(geometry)) {
                    return geometry;
                }
                result = convert(target);
                break;
            }
            case ST_Is3D: {
                final Coordinate c = geometry.getCoordinate();
                return (c != null) ? !Double.isNaN(c.z) : null;
            }
            case ST_CoordDim: {
                final Coordinate c = geometry.getCoordinate();
                return (c != null) ? Double.isNaN(c.z) ? 2 : 3 : null;
            }
            case ST_GeometryType: {
                for (int i=0; i < TYPES.length; i++) {
                    if (TYPES[i].isInstance(geometry)) {
                        return SQLMM_NAMES[i];
                    }
                }
                return null;
            }
            case ST_ExplicitPoint: {
                final Coordinate c = ((Point) geometry).getCoordinate();
                if (c == null) return ArraysExt.EMPTY_DOUBLE;
                final double x = c.getX();
                final double y = c.getY();
                final double z = c.getZ();
                return Double.isNaN(z) ? new double[] {x, y} : new double[] {x, y, z};
            }
            case ST_Simplify: {
                final double distance = ((Number) argument).doubleValue();
                result = DouglasPeuckerSimplifier.simplify(geometry, distance);
                break;
            }
            case ST_SimplifyPreserveTopology: {
                final double distance = ((Number) argument).doubleValue();
                result = TopologyPreservingSimplifier.simplify(geometry, distance);
                break;
            }
            default: return super.operationSameCRS(operation, other, argument);
        }
        JTS.copyMetadata(geometry, result);
        return result;
    }

    /**
     * The types of JTS objects to be recognized by the SQLMM {@code ST_GeometryType} operation.
     */
    private static final Class<?>[] TYPES = {
        Point.class, LineString.class, Polygon.class,
        MultiPoint.class, MultiLineString.class, MultiPolygon.class,
        GeometryCollection.class, Geometry.class,
    };

    /**
     * The SQLMM names for the types listed in the {@link #TYPES} array.
     */
    private static final String[] SQLMM_NAMES = {
        "ST_Point", "ST_LineString", "ST_Polygon",
        "ST_MultiPoint", "ST_MultiLineString", "ST_MultiPolygon",
        "ST_GeomCollection", "ST_Geometry"
    };

    /**
     * Converts the given argument to a zero-based index.
     *
     * @throws ClassCastException if the argument is not a string or a number.
     * @throws NumberFormatException if the argument is an unparseable string.
     * @throws IllegalArgumentException if the argument is zero or negative.
     */
    private static int toIndex(final Object argument) {
        final int i = (argument instanceof CharSequence)
                ? Integer.parseInt(argument.toString())
                : ((Number) argument).intValue();           // ClassCastException is part of this method contract.
        ArgumentChecks.ensureStrictlyPositive("index", i);
        return i - 1;
    }

    /**
     * Converts the given geometry to the specified type.
     * If the geometry is already of that type, it is returned unchanged.
     * Otherwise coordinates are copied in a new geometry of the requested type.
     *
     * <p>The following conversions are illegal and will cause an {@link IllegalArgumentException} to be thrown:</p>
     * <ul>
     *   <li>From point to polyline or polygon (exception thrown by JTS itself).</li>
     *   <li>From geometry collection (except multi-point) to polyline.</li>
     *   <li>From geometry collection (except multi-point and multi-line string) to polygon.</li>
     *   <li>From geometry collection containing nested collections.</li>
     * </ul>
     *
     * The conversion from {@link MultiLineString} to {@link Polygon} is defined as following:
     * the first {@link LineString} is taken as the exterior {@link LinearRing} and all others
     * {@link LineString}s are interior {@link LinearRing}s.
     * This rule is defined by some SQLMM operations.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    @Override
    public GeometryWrapper<Geometry> toGeometryType(final GeometryType target) {
        if (!factory().getGeometryClass(target).isInstance(geometry)) {
            final Geometry result = convert(target);
            if (result != geometry) {
                JTS.copyMetadata(geometry, result);
                return new Wrapper(result);
            }
        }
        return this;
    }

    /**
     * Converts the given geometry to the specified type without wrapper.
     * This is the implementation of {@link #toGeometryType(GeometryType)}.
     * Caller should invoke {@link JTS#copyMetadata(Geometry, Geometry)} after this method.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    private Geometry convert(final GeometryType target) {
        final GeometryFactory factory = geometry.getFactory();
        switch (target) {
            case POINT: {
                return geometry.getCentroid();
            }
            case LINESTRING: {
                if (isCollection(geometry)) break;
                return factory.createLineString(geometry.getCoordinates());
            }
            case POLYGON: {
                if (!geometry.isEmpty() && geometry instanceof MultiLineString) {
                    // SQLMM `ST_BdMPolyFromText` and `ST_BdMPolyFromWKB` behavior.
                    final MultiLineString lines  = (MultiLineString) geometry;
                    final LinearRing   exterior  = factory.createLinearRing(lines.getGeometryN(0).getCoordinates());
                    final LinearRing[] interiors = new LinearRing[lines.getNumGeometries() - 1];
                    for (int i=0; i < interiors.length;) {
                        interiors[i] = factory.createLinearRing(lines.getGeometryN(++i).getCoordinates());
                    }
                    return factory.createPolygon(exterior, interiors);
                }
                if (isCollection(geometry)) break;
                return factory.createPolygon(geometry.getCoordinates());
            }
            case MULTI_POINT: {
                return (geometry instanceof Point)
                        ? factory.createMultiPoint(new Point[] {(Point) geometry})
                        : factory.createMultiPointFromCoords(geometry.getCoordinates());
            }
            case MULTI_LINESTRING: {
                return toCollection(factory,
                        LineString.class, LineString[]::new,
                        GeometryFactory::createLineString,
                        GeometryFactory::createMultiLineString);
            }
            case MULTI_POLYGON: {
                return toCollection(factory,
                        Polygon.class, Polygon[]::new,
                        GeometryFactory::createPolygon,
                        GeometryFactory::createMultiPolygon);
            }
            case GEOMETRY_COLLECTION: {
                if (geometry instanceof Point) {
                    return factory.createMultiPoint(new Point[] {(Point) geometry});
                } else if (geometry instanceof LineString) {
                    return factory.createMultiLineString(new LineString[] {(LineString) geometry});
                } else if (geometry instanceof Polygon) {
                    return factory.createMultiPolygon(new Polygon[] {(Polygon) geometry});
                }
                break;
            }
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                geometry.getClass(), factory().getGeometryClass(target)));
    }

    /**
     * Converts a single geometry or a geometry collection to a collection of another type.
     * This is a helper method for {@link #toGeometryType(GeometryType)}.
     *
     * @param  <T>             the compile-time value of {@code type}.
     * @param  factory         the factory to use for creating new geometries.
     * @param  type            the type of geometry components to put in a collection.
     * @param  newArray        constructor for a new array of given {@code type}.
     * @param  newComponent    constructor for a geometry component of given {@code type}.
     * @param  newCollection   constructor for a geometry collection from an array of components/
     * @return the geometry collection created from the given type.
     * @throws IllegalArgumentException if a geometry collection contains nested collection.
     */
    private <T extends Geometry> GeometryCollection toCollection(
            final GeometryFactory factory,
            final Class<T> type, final IntFunction<T[]> newArray,
            final BiFunction<GeometryFactory,Coordinate[],T> newComponent,
            final BiFunction<GeometryFactory,T[],GeometryCollection> newCollection)
    {
        final T[] components = newArray.apply(geometry.getNumGeometries());
        for (int i=0; i<components.length; i++) {
            final Geometry c = geometry.getGeometryN(i);
            if (type.isInstance(c)) {
                components[i] = type.cast(c);
            } else if (isCollection(c)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NestedElementNotAllowed_1, GeometryCollection.class));
            } else {
                components[i] = newComponent.apply(factory, c.getCoordinates());
            }
        }
        return newCollection.apply(factory, components);
    }

    /**
     * Returns {@code true} if the given geometry is a collection other than {@link MultiPoint}.
     * Collections are handled recursively by {@code getLineStrings(…)} and {@code getPolygons(…)}.
     */
    private static boolean isCollection(final Geometry geometry) {
        return (geometry.getNumGeometries() >= 2) && !(geometry instanceof MultiPoint);
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the operation is {@code null}, then the geometry is returned unchanged.
     * If the geometry uses a different CRS than the source CRS of the given operation
     * and {@code validate} is {@code true},
     * then a new operation to the target CRS will be automatically computed.
     *
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  validate   whether to validate the operation source CRS.
     * @throws FactoryException if transformation to the target CRS cannot be found.
     * @throws TransformException if the geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper<Geometry> transform(final CoordinateOperation operation, final boolean validate)
            throws FactoryException, TransformException
    {
        return rewrap(JTS.transform(geometry, operation, validate));
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null or is the same CRS than current one, the geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, then the geometry is returned unchanged.
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws TransformException if this geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper<Geometry> transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        try {
            return rewrap(JTS.transform(geometry, targetCRS));
        } catch (FactoryException e) {
            /*
             * We wrap that exception because `Geometry.transform(…)` does not declare `FactoryException`.
             * We may revisit in a future version if `Geometry.transform(…)` method declaration is updated.
             */
            throw new TransformException(e);
        }
    }

    /**
     * Returns {@code true} if the given geometry use the same CRS than this geometry, or conservatively
     * returns {@code false} in case of doubt. This method should perform only a cheap test; it is used
     * as a way to filter rapidly if {@link #transform(CoordinateReferenceSystem)} needs to be invoked.
     *
     * @param  other  the second geometry.
     * @return {@code true} if the two geometries use equivalent CRS or if the CRS is undefined on both side,
     *         or {@code false} in case of doubt.
     */
    @Override
    public boolean isSameCRS(final GeometryWrapper<Geometry> other) {
        return JTS.isSameCRS(geometry, ((Wrapper) other).geometry);
    }

    /**
     * Returns the WKT representation of the wrapped geometry.
     */
    @Override
    public String formatWKT(final double flatness) {
        return geometry.toText();
    }
}
