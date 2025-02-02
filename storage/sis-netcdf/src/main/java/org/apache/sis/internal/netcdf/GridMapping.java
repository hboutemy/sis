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
package org.apache.sis.internal.netcdf;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Supplier;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.referencing.provider.PseudoPlateCarree;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Numbers;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.measure.Units;
import ucar.nc2.constants.CF;

// Branch-dependent imports
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;


/**
 * Temporary objects for creating a {@link GridGeometry} instance defined by attributes on a variable.
 * Those attributes are defined by CF-conventions, but some other non-CF attributes are also in usage
 * (e.g. GDAL or ESRI conventions). This class uses a different approach than {@link CRSBuilder},
 * which creates Coordinate Reference Systems by inspecting coordinate system axes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
 *
 * @since 1.0
 */
final class GridMapping {
    /**
     * The Coordinate Reference System inferred from grid mapping attribute values, or {@code null} if none.
     * This CRS may have been constructed from Well Known Text or EPSG codes declared in {@code "spatial_ref"},
     * {@code "ESRI_pe_string"} or {@code "EPSG_code"} attributes.
     *
     * <h4>Usage note</h4>
     * This come from different information than the one used by {@link CRSBuilder},
     * which creates CRS by inspection of coordinate system axes.
     */
    final CoordinateReferenceSystem crs;

    /**
     * The <cite>grid to CRS</cite> transform, or {@code null} if none. This information is usually not specified
     * except when using GDAL conventions. If {@code null}, then the transform should be inferred by {@link Grid}.
     */
    private final MathTransform gridToCRS;

    /**
     * Whether the {@link #crs} was defined by a WKT string.
     */
    private final boolean isWKT;

    /**
     * Creates an instance for the given {@link #crs} and {@link #gridToCRS} values.
     *
     * @param  crs        CRS inferred from grid mapping attribute values, or {@code null} if none.
     * @param  gridToCRS  transform from GDAL conventions, or {@code null} if none.
     * @param  isWKT      wether the {@code crs} was defined by a WKT string.
     */
    private GridMapping(final CoordinateReferenceSystem crs, final MathTransform gridToCRS, final boolean isWKT) {
        this.crs       = crs;
        this.gridToCRS = gridToCRS;
        this.isWKT     = isWKT;
    }

    /**
     * Fetches grid geometry information from attributes associated to the given variable.
     * This method should be invoked only once per variable, but may return a shared {@code GridMapping} instance
     * for all variables because there is typically only one set of grid mapping attributes for the whole file.
     *
     * @param  variable  the variable for which to create a grid geometry.
     */
    static GridMapping forVariable(final Variable variable) {
        final Map<Object,GridMapping> gridMapping = variable.decoder.gridMapping;
        for (final String name : variable.decoder.convention().nameOfMappingNode(variable)) {
            GridMapping gm = gridMapping.get(name);
            if (gm != null) {
                return gm;
            }
            /*
             * Value may be null if we already tried and failed to process that grid.
             * We detect those cases in order to avoid logging the same warning twice.
             */
            if (!gridMapping.containsKey(name)) {
                final Node mapping = variable.decoder.findNode(name);
                if (mapping != null) {
                    gm = parse(mapping);
                }
                gridMapping.put(name, gm);                      // Store even if null.
                if (gm != null) {
                    return gm;
                }
            }
        }
        /*
         * Found no "grid_mapping" attribute. The block below is not CF-compliant,
         * but we find some use of this non-standard approach in practice.
         */
        GridMapping gm = gridMapping.get(variable);
        if (gm == null) {
            final String name = variable.getName();
            gm = gridMapping.get(name);
            if (gm == null && !gridMapping.containsKey(name)) {
                gm = parse(variable);
                gridMapping.put(name, gm);                      // Store even if null.
            }
            if (gm == null) {
                gm = parseNonStandard(variable);
            }
            if (gm != null) {
                gridMapping.put(variable, gm);
            }
        }
        return gm;
    }

    /**
     * Parses the map projection parameters defined as attribute associated to the given variable.
     * This method tries to parse CF-compliant attributes first. If none are found, non-standard
     * extensions (for example GDAL usage) are tried next.
     */
    private static GridMapping parse(final Node mapping) {
        GridMapping gm = parseProjectionParameters(mapping);
        if (gm == null) {
            gm = parseGeoTransform(mapping);
        }
        return gm;
    }

    /**
     * If the netCDF variable defines explicitly the map projection method and its parameters, returns those parameters.
     * Otherwise returns {@code null}. The given {@code node} argument is typically a dummy variable referenced by value
     * of the {@value CF#GRID_MAPPING} attribute on the real data variable (as required by CF-conventions), but may also
     * be something else (the data variable itself, or a group, <i>etc.</i>). That node, together with the attributes to
     * be parsed, depends on the {@link Convention} instance.
     *
     * @param  node  the dummy variable on which attributes are defined for projection parameters.
     *
     * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
     */
    private static GridMapping parseProjectionParameters(final Node node) {
        final Map<String,Object> definition = node.decoder.convention().projection(node);
        if (definition != null) try {
            /*
             * Fetch now numerical values that are not map projection parameters.
             * This step needs to be done before to try to set parameter values.
             */
            final Object greenwichLongitude = definition.remove(Convention.LONGITUDE_OF_PRIME_MERIDIAN);
            /*
             * Prepare the block of projection parameters. The set of legal parameter depends on the map projection.
             * We assume that all numerical values are map projection parameters; character sequences (assumed to be
             * component names) are handled later. The CF-conventions use parameter names that are slightly different
             * than OGC names, but Apache SIS implementations of map projections know how to handle them, including
             * the redundant parameters like "inverse_flattening" and "earth_radius".
             */
            final DefaultCoordinateOperationFactory opFactory = node.decoder.getCoordinateOperationFactory();
            final OperationMethod method = opFactory.getOperationMethod((String) definition.remove(CF.GRID_MAPPING_NAME));
            final ParameterValueGroup parameters = method.getParameters().createValue();
            for (final Iterator<Map.Entry<String,Object>> it = definition.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<String,Object> entry = it.next();
                final String name  = entry.getKey();
                final Object value = entry.getValue();
                try {
                    if (value instanceof Number || value instanceof double[] || value instanceof float[]) {
                        it.remove();
                        parameters.parameter(name).setValue(value);
                    } else if (value instanceof String && !name.endsWith(Convention.NAME_SUFFIX)) {
                        /*
                         * In principle we should ignore non-numeric parameters. But in practice, some badly encoded
                         * netCDF files store parameters as strings instead of numbers. If the parameter name is
                         * known to the projection method, try to parse the character string.
                         */
                        final ParameterValue<?> parameter;
                        try {
                            parameter = parameters.parameter(name);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        final Class<?> type = parameter.getDescriptor().getValueClass();
                        if (Numbers.isNumber(type)) {
                            it.remove();
                            parameter.setValue(Double.parseDouble((String) value));
                        } else if (Numbers.isNumber(type.getComponentType())) {
                            it.remove();
                            parameter.setValue(parseDoubles((String) value), null);
                        }
                    }
                } catch (IllegalArgumentException ex) {                     // Includes NumberFormatException.
                    warning(node, ex, Resources.Keys.CanNotSetProjectionParameter_5, node.decoder.getFilename(),
                            node.getName(), name, value, ex.getLocalizedMessage());
                }
            }
            /*
             * In principle, projection parameters do not include the semi-major and semi-minor axis lengths.
             * But if those information are provided, then we use them for building the geodetic reference frame.
             * Otherwise a default reference frame will be used.
             */
            final GeographicCRS baseCRS = createBaseCRS(node.decoder, parameters, definition, greenwichLongitude);
            final MathTransform baseToCRS;
            final CoordinateReferenceSystem crs;
            if (method instanceof PseudoPlateCarree) {
                // Only swap axis order from (latitude, longitude) to (longitude, latitude).
                baseToCRS = MathTransforms.linear(new Matrix3(0, 1, 0, 1, 0, 0, 0, 0, 1));
                crs = baseCRS;
            } else {
                Map<String,?> properties = properties(definition, Convention.CONVERSION_NAME, node.getName());
                final Conversion conversion = opFactory.createDefiningConversion(properties, method, parameters);
                final CartesianCS cs = node.decoder.getStandardProjectedCS();
                properties = properties(definition, Convention.PROJECTED_CRS_NAME, conversion);
                final ProjectedCRS p = node.decoder.getCRSFactory().createProjectedCRS(properties, baseCRS, conversion, cs);
                baseToCRS = p.getConversionFromBase().getMathTransform();
                crs = p;
            }
            /*
             * Report all projection parameters that have not been used. If the map is not rendered
             * at expected location, it may be because we have ignored some important parameters.
             */
            if (!definition.isEmpty()) {
                warning(node, null, Resources.Keys.UnknownProjectionParameters_2,
                        node.decoder.getFilename(), String.join(", ", definition.keySet()));
            }
            /*
             * Build the "grid to CRS" if present. This is not defined by CF-convention,
             * but may be present in some non-CF conventions.
             */
            final MathTransform gridToCRS = node.decoder.convention().gridToCRS(node, baseToCRS);
            return new GridMapping(crs, gridToCRS, false);
        } catch (ClassCastException | IllegalArgumentException | FactoryException | TransformException e) {
            canNotCreate(node, Resources.Keys.CanNotCreateCRS_3, e);
        }
        return null;
    }

    /**
     * Creates the geographic CRS from axis length specified in the given map projection parameters.
     * The returned CRS will always have (latitude, longitude) axes in that order and in degrees.
     *
     * @param  parameters  parameters from which to get ellipsoid axis lengths. Will not be modified.
     * @param  definition  map from which to get element names. Elements used will be removed.
     */
    private static GeographicCRS createBaseCRS(final Decoder decoder, final ParameterValueGroup parameters,
            final Map<String,Object> definition, final Object greenwichLongitude) throws FactoryException
    {
        final DatumFactory datumFactory = decoder.getDatumFactory();
        final CommonCRS defaultDefinitions = decoder.convention().defaultHorizontalCRS(false);
        boolean isSpecified = false;
        /*
         * Prime meridian built from "longitude_of_prime_meridian".
         */
        final PrimeMeridian meridian;
        if (greenwichLongitude instanceof Number) {
            final double longitude = ((Number) greenwichLongitude).doubleValue();
            final Map<String,?> properties = properties(definition,
                    Convention.PRIME_MERIDIAN_NAME, (longitude == 0) ? "Greenwich" : null);
            meridian = datumFactory.createPrimeMeridian(properties, longitude, Units.DEGREE);
            isSpecified = true;
        } else {
            meridian = defaultDefinitions.primeMeridian();
        }
        /*
         * Ellipsoid built from "semi_major_axis" and "semi_minor_axis" parameters. Note that it is okay
         * to use the OGC name (e.g. "semi_major") instead of the netCDF name (e.g. ""semi_major_axis").
         * The Apache SIS implementation of parameter value group understands the aliases. Using the OGC
         * names is safer because they should be understood by most map projection implementations.
         */
        Ellipsoid ellipsoid;
        try {
            final ParameterValue<?> p = parameters.parameter(Constants.SEMI_MAJOR);
            final Unit<Length> axisUnit = p.getUnit().asType(Length.class);
            final double  semiMajor = p.doubleValue();
            final double  secondDefiningParameter;
            final boolean isSphere;
            final boolean isIvfDefinitive = parameters.parameter(Constants.IS_IVF_DEFINITIVE).booleanValue();
            if (isIvfDefinitive) {
                secondDefiningParameter = parameters.parameter(Constants.INVERSE_FLATTENING).doubleValue();
                isSphere = (secondDefiningParameter == 0) || Double.isInfinite(secondDefiningParameter);
            } else {
                secondDefiningParameter = parameters.parameter(Constants.SEMI_MINOR).doubleValue(axisUnit);
                isSphere = secondDefiningParameter == semiMajor;
            }
            final Supplier<Object> fallback = () -> {           // Default ellipsoid name if not specified.
                final Locale  locale = decoder.listeners.getLocale();
                final NumberFormat f = NumberFormat.getNumberInstance(locale);
                f.setMaximumFractionDigits(5);      // Centimetric precision.
                final double km = axisUnit.getConverterTo(Units.KILOMETRE).convert(semiMajor);
                final StringBuffer b = new StringBuffer()
                        .append(Vocabulary.getResources(locale).getString(isSphere ? Vocabulary.Keys.Sphere : Vocabulary.Keys.Ellipsoid))
                        .append(isSphere ? " R=" : " a=");
                return f.format(km, b, new FieldPosition(0)).append(" km").toString();
            };
            final Map<String,?> properties = properties(definition, Convention.ELLIPSOID_NAME, fallback);
            if (isIvfDefinitive) {
                ellipsoid = datumFactory.createFlattenedSphere(properties, semiMajor, secondDefiningParameter, axisUnit);
            } else {
                ellipsoid = datumFactory.createEllipsoid(properties, semiMajor, secondDefiningParameter, axisUnit);
            }
            isSpecified = true;
        } catch (ParameterNotFoundException | IllegalStateException e) {
            // Ignore - may be normal if the map projection is not an Apache SIS implementation.
            ellipsoid = defaultDefinitions.ellipsoid();
        }
        /*
         * Geodetic datum built from "towgs84" and above properties.
         */
        final Object bursaWolf = definition.remove(Convention.TOWGS84);
        final GeodeticDatum datum;
        if (isSpecified | bursaWolf != null) {
            Map<String,Object> properties = properties(definition, Convention.GEODETIC_DATUM_NAME, ellipsoid);
            if (bursaWolf instanceof BursaWolfParameters) {
                properties = new HashMap<>(properties);
                properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, bursaWolf);
                isSpecified = true;
            }
            datum = datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
        } else {
            datum = defaultDefinitions.datum();
        }
        /*
         * Geographic CRS from all above properties.
         */
        if (isSpecified) {
            final Map<String,?> properties = properties(definition, Convention.GEOGRAPHIC_CRS_NAME, datum);
            return decoder.getCRSFactory().createGeographicCRS(properties, datum,
                    defaultDefinitions.geographic().getCoordinateSystem());
        } else {
            return defaultDefinitions.geographic();
        }
    }

    /**
     * Returns the {@code properties} argument value to give to the factory methods of geodetic objects.
     * The returned map contains at least an entry for {@value IdentifiedObject#NAME_KEY} with the name
     * fetched from the value of the attribute named {@code nameAttribute}.
     *
     * @param definition     map containing the attribute values.
     * @param nameAttribute  name of the attribute from which to get the name.
     * @param fallback       fallback as an {@link IdentifiedObject} (from which the name will be copied),
     *                       or a character sequence, or {@code null} for "Unnamed" localized string.
     */
    private static Map<String,Object> properties(final Map<String,Object> definition, final String nameAttribute, final Object fallback) {
        Object name = definition.remove(nameAttribute);
        if (name == null) {
            if (fallback == null) {
                // Note: IdentifiedObject.name does not accept InternationalString.
                name = Vocabulary.format(Vocabulary.Keys.Unnamed);
            } else if (fallback instanceof IdentifiedObject) {
                name = ((IdentifiedObject) fallback).getName();
            } else if (fallback instanceof Supplier<?>) {
                name = ((Supplier<?>) fallback).get();
            } else {
                name = fallback.toString();
            }
        }
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Tries to parse a CRS and affine transform from GDAL GeoTransform coefficients.
     * Those coefficients are not in the usual order expected by matrix, affine
     * transforms or TFW files. The relationship from pixel/line (P,L) coordinates
     * to CRS are:
     *
     * {@snippet lang="java" :
     *     X = c[0] + P*c[1] + L*c[2];
     *     Y = c[3] + P*c[4] + L*c[5];
     *     }
     *
     * @param  mapping  the variable that contains attributes giving CRS definition.
     * @return the mapping, or {@code null} if this method did not found grid geometry attributes.
     */
    private static GridMapping parseGeoTransform(final Node mapping) {
        final String wkt = mapping.getAttributeAsString("spatial_ref");
        final String gtr = mapping.getAttributeAsString("GeoTransform");
        if (wkt == null && gtr == null) {
            return null;
        }
        short message = Resources.Keys.CanNotCreateCRS_3;
        CoordinateReferenceSystem crs = null;
        MathTransform gridToCRS = null;
        try {
            if (wkt != null) {
                crs = createFromWKT(mapping, wkt);
            }
            if (gtr != null) {
                message = Resources.Keys.CanNotCreateGridGeometry_3;
                final double[] c = parseDoubles(gtr);
                if (c.length == 6) {
                    gridToCRS = new AffineTransform2D(c[1], c[4], c[2], c[5], c[0], c[3]);         // X_DIMENSION, Y_DIMENSION
                } else {
                    canNotCreate(mapping, message, new DataStoreContentException(
                            Errors.getResources(mapping.getLocale())
                                  .getString(Errors.Keys.UnexpectedArrayLength_2, 6, c.length)));
                }
            }
        } catch (ParseException | NumberFormatException e) {
            canNotCreate(mapping, message, e);
        }
        return new GridMapping(crs, gridToCRS, wkt != null);
    }

    /**
     * Parses a comma-separated or space-separated array of numbers.
     *
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    private static double[] parseDoubles(final String values) {
        return CharSequences.parseDoubles(values.replace(',', ' '), ' ');
    }

    /**
     * Tries to parse the Coordinate Reference System using ESRI conventions or other non-CF conventions.
     * This method is invoked as a fallback if {@link #parseGeoTransform(Node)} found no grid geometry.
     *
     * @param  variable  the variable potentially with attributes to parse.
     * @return whether this method found grid geometry attributes.
     */
    private static GridMapping parseNonStandard(final Node variable) {
        String code = variable.getAttributeAsString("ESRI_pe_string");
        final boolean isWKT = (code != null);
        if (!isWKT) {
            code = variable.getAttributeAsString("EPSG_code");
            if (code == null) {
                return null;
            }
        }
        /*
         * The Coordinate Reference System stored in those attributes often use the GeoTIFF flavor of EPSG codes,
         * with (longitude, latitude) axis order instead of the authoritative order specified in EPSG database.
         * Likewise, the "WKT 1" flavor used by ESRI is different than WKT 1 defined by OGC 01-009 specification.
         * The CRS parsings below need to take those differences in account, except axis order which is tested in
         * the `adaptGridCRS(…)` method.
         */
        CoordinateReferenceSystem crs;
        try {
            if (isWKT) {
                crs = createFromWKT(variable, code);
            } else {
                crs = CRS.forCode(Constants.EPSG + ':' + code);
            }
        } catch (FactoryException | ParseException | ClassCastException e) {
            canNotCreate(variable, Resources.Keys.CanNotCreateCRS_3, e);
            crs = null;
        }
        return new GridMapping(crs, null, isWKT);
    }

    /**
     * Creates a coordinate reference system by parsing a Well Known Text (WKT) string. The WKT is presumed
     * to use the GDAL flavor of WKT 1, and warnings are redirected to decoder listeners.
     */
    private static CoordinateReferenceSystem createFromWKT(final Node node, final String wkt) throws ParseException {
        final WKTFormat f = new WKTFormat(node.getLocale(), node.decoder.getTimeZone());
        f.setConvention(org.apache.sis.io.wkt.Convention.WKT1_COMMON_UNITS);
        final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) f.parseObject(wkt);
        final Warnings warnings = f.getWarnings();
        if (warnings != null) {
            final LogRecord record = new LogRecord(Level.WARNING, warnings.toString());
            record.setLoggerName(Modules.NETCDF);
            record.setSourceClassName(Variable.class.getCanonicalName());
            record.setSourceMethodName("getGridGeometry");
            node.decoder.listeners.warning(record);
        }
        return crs;
    }

    /**
     * Logs a warning about a CRS or grid geometry that cannot be created.
     * This method presumes that {@link GridMapping} are invoked (indirectly) from {@link Variable#getGridGeometry()}.
     *
     * @param  key  one of {@link Resources.Keys#CanNotCreateCRS_3} or {@link Resources.Keys#CanNotCreateGridGeometry_3}.
     * @param  ex   the exception that occurred while creating the CRS or grid geometry.
     */
    private static void canNotCreate(final Node node, final short key, final Exception ex) {
        warning(node, ex, key, node.decoder.getFilename(), node.getName(), ex.getLocalizedMessage());
    }

    /**
     * Logs a warning, presuming that {@link GridMapping} are invoked (indirectly) from {@link Variable#getGridGeometry()}.
     */
    private static void warning(final Node node, final Exception ex, final short key, final Object... arguments) {
        NamedElement.warning(node.decoder.listeners, Variable.class, "getGridGeometry", ex, null, key, arguments);
    }

    /**
     * Creates a new grid geometry with the extent of the given variable and a potentially null CRS.
     * This method should be invoked only as a fallback when no existing {@link GridGeometry} can be used.
     * The CRS and "grid to CRS" transform are null, unless some partial information was found for example
     * as WKT string.
     */
    final GridGeometry createGridCRS(final Variable variable) {
        final List<Dimension> dimensions = variable.getGridDimensions();
        final long[] upper = new long[dimensions.size()];
        for (int i=0; i<upper.length; i++) {
            final int d = (upper.length - 1) - i;           // Convert CRS dimension to netCDF dimension.
            upper[i] = dimensions.get(d).length();
        }
        return new GridGeometry(new GridExtent(null, null, upper, false), PixelInCell.CELL_CENTER, gridToCRS, crs);
    }

    /**
     * Creates the grid geometry from the {@link #crs} and {@link #gridToCRS} fields,
     * completing missing information with the implicit grid geometry derived from coordinate variables.
     * For example, {@code GridMapping} may contain information only about the horizontal dimensions, so
     * the given {@code implicit} geometry is used for completing with vertical and temporal dimensions.
     *
     * @param  variable  the variable for which to create a grid geometry.
     * @param  implicit  template to use for completing missing information.
     * @param  anchor    whether we computed "grid to CRS" transform relative to pixel center or pixel corner.
     * @return the grid geometry with modified CRS and "grid to CRS" transform, or {@code null} in case of failure.
     */
    final GridGeometry adaptGridCRS(final Variable variable, final GridGeometry implicit, final PixelInCell anchor) {
        /*
         * The CRS and grid geometry built from grid mapping attributes are called "explicit" in this method.
         * This is by contrast with CRS derived from coordinate variables, which is only implicit.
         */
        CoordinateReferenceSystem explicitCRS = crs;
        int firstAffectedCoordinate = 0;
        boolean isSameGrid = true;
        if (implicit.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem implicitCRS = implicit.getCoordinateReferenceSystem();
            if (explicitCRS == null) {
                explicitCRS = implicitCRS;
            } else {
                /*
                 * The CRS built by the `Grid` class (based on an inspection of coordinate variables)
                 * may have a different axis order than the CRS specified by grid mapping attributes
                 * (the CRS built by this class). This block checks which axis order seems to fit,
                 * then potentially replaces `Grid` implicit CRS by `GridMapping` explicit CRS.
                 *
                 * This is where the potential difference between EPSG axis order and grid axis order is handled.
                 * If we cannot find which component to replace, assume that grid mapping describes the first dimensions.
                 * We have no guarantees that this latter assumption is right, but it seems to match common practice.
                 */
                final CoordinateSystem cs = implicitCRS.getCoordinateSystem();
                firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, explicitCRS.getCoordinateSystem());
                if (firstAffectedCoordinate < 0) {
                    explicitCRS = AbstractCRS.castOrCopy(explicitCRS).forConvention(AxesConvention.RIGHT_HANDED);
                    firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, explicitCRS.getCoordinateSystem());
                    if (firstAffectedCoordinate < 0) {
                        firstAffectedCoordinate = 0;
                        if (isWKT && crs != null) {
                            explicitCRS = crs;                         // If specified by WKT, use the CRS verbatim.
                        }
                    }
                }
                /*
                 * Replace the grid CRS (or a component of it) by the CRS parsed from WKT or EPSG code with same (if possible)
                 * axis order. If the grid CRS contains more axes (for example elevation or time axis), we try to keep them.
                 */
                try {
                    explicitCRS = new CRSMerger(variable.decoder)
                            .replaceComponent(implicitCRS, firstAffectedCoordinate, explicitCRS);
                } catch (FactoryException e) {
                    canNotCreate(variable, Resources.Keys.CanNotCreateCRS_3, e);
                    return null;
                }
                isSameGrid = implicitCRS.equals(explicitCRS);
                if (isSameGrid) {
                    explicitCRS = implicitCRS;                                 // Keep existing instance if appropriate.
                }
            }
        }
        /*
         * Perform the same substitution than above, but in the "grid to CRS" transform. Note that the "grid to CRS"
         * is usually not specified, so the block performing substitution will rarely be executed. If executed, then
         * then we need to perform selection in target dimensions (not source dimensions) because the first affected
         * coordinate computed above is in CRS dimension, which is the target of "grid to CRS" transform.
         */
        MathTransform explicitG2C = gridToCRS;
        if (implicit.isDefined(GridGeometry.GRID_TO_CRS)) {
            final MathTransform implicitG2C = implicit.getGridToCRS(anchor);
            if (explicitG2C == null) {
                explicitG2C = implicitG2C;
            } else try {
                int count = 0;
                MathTransform[] components = new MathTransform[3];
                final TransformSeparator sep = new TransformSeparator(implicitG2C, variable.decoder.getMathTransformFactory());
                if (firstAffectedCoordinate != 0) {
                    sep.addTargetDimensionRange(0, firstAffectedCoordinate);
                    components[count++] = sep.separate();
                    sep.clear();
                }
                components[count++] = explicitG2C;
                final int next = firstAffectedCoordinate + explicitG2C.getTargetDimensions();
                final int upper = implicitG2C.getTargetDimensions();
                if (next != upper) {
                    sep.addTargetDimensionRange(next, upper);
                    components[count++] = sep.separate();
                }
                components = ArraysExt.resize(components, count);
                explicitG2C = MathTransforms.compound(components);
                if (implicitG2C.equals(explicitG2C)) {
                    explicitG2C = implicitG2C;                                 // Keep using existing instance if appropriate.
                } else {
                    isSameGrid = false;
                }
            } catch (FactoryException e) {
                canNotCreate(variable, Resources.Keys.CanNotCreateGridGeometry_3, e);
                return null;
            }
        }
        /*
         * At this point we finished to compute the grid geometry components.
         * If any of them have changed, create the new grid geometry.
         */
        if (isSameGrid) {
            return implicit;
        } else {
            return new GridGeometry(implicit.getExtent(), anchor, explicitG2C, explicitCRS);
        }
    }
}
