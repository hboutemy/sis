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
package org.apache.sis.internal.referencing.provider;

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.PlanarProjection;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for <cite>"Lambert Azimuthal Equal Area"</cite> projection (EPSG:9820).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see <a href="http://geotiff.maptools.org/proj_list/lambert_azimuthal_equal_area.html">GeoTIFF parameters for Lambert Azimuthal Equal Area</a>
 *
 * @since 1.2
 */
@XmlTransient
public class LambertAzimuthalEqualArea extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3360095493388421774L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     * The {@code IDENTIFIER_OF_BASE} name is for avoiding confusion with
     * the {@code IDENTIFIER} name used in subclasses.
     */
    public static final String IDENTIFIER_OF_BASE = "9820";

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values range is [-90 … 90]°. The default value is 0°
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Latitude of natural origin </td></tr>
     *   <tr><td> OGC:     </td><td> latitude_of_center </td></tr>
     *   <tr><td> ESRI:    </td><td> Latitude_Of_Origin </td></tr>
     *   <tr><td> NetCDF:  </td><td> latitude_of_projection_origin </td></tr>
     *   <tr><td> GeoTIFF: </td><td> CenterLat </td></tr>
     *   <tr><td> Proj4:   </td><td> lat_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Longitude of natural origin </td></tr>
     *   <tr><td> OGC:     </td><td> longitude_of_center </td></tr>
     *   <tr><td> ESRI:    </td><td> Central_Meridian </td></tr>
     *   <tr><td> NetCDF:  </td><td> longitude_of_projection_origin </td></tr>
     *   <tr><td> GeoTIFF: </td><td> CenterLong </td></tr>
     *   <tr><td> Proj4:   </td><td> lon_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> False easting </td></tr>
     *   <tr><td> OGC:     </td><td> false_easting </td></tr>
     *   <tr><td> ESRI:    </td><td> False_Easting </td></tr>
     *   <tr><td> NetCDF:  </td><td> false_easting </td></tr>
     *   <tr><td> GeoTIFF: </td><td> FalseEasting </td></tr>
     *   <tr><td> Proj4:   </td><td> x_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING = Equirectangular.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> False northing </td></tr>
     *   <tr><td> OGC:     </td><td> false_northing </td></tr>
     *   <tr><td> ESRI:    </td><td> False_Northing </td></tr>
     *   <tr><td> NetCDF:  </td><td> false_northing </td></tr>
     *   <tr><td> GeoTIFF: </td><td> FalseNorthing </td></tr>
     *   <tr><td> Proj4:   </td><td> y_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Equirectangular.FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LATITUDE_OF_ORIGIN = createLatitude(renameAlias(builder, Equirectangular.LATITUDE_OF_ORIGIN,
                                            Citations.OGC, AlbersEqualArea.LATITUDE_OF_FALSE_ORIGIN), true);

        LONGITUDE_OF_ORIGIN = createLongitude(renameAlias(builder, Equirectangular.LONGITUDE_OF_ORIGIN,
                                              Citations.OGC, AlbersEqualArea.LONGITUDE_OF_FALSE_ORIGIN));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER_OF_BASE)
                .addName(                    "Lambert Azimuthal Equal Area")
                .addName(Citations.OGC,      "Lambert_Azimuthal_Equal_Area")
                .addName(Citations.ESRI,     "Lambert_Azimuthal_Equal_Area")
                .addName(Citations.NETCDF,   "LambertAzimuthalEqualArea")
                .addName(Citations.GEOTIFF,  "CT_LambertAzimEqualArea")
                .addName(Citations.PROJ4,    "laea")
                .addIdentifier(Citations.GEOTIFF, "10")
                .addIdentifier(Citations.MAP_INFO, "4")
                .createGroupForMapProjection(
                        LATITUDE_OF_ORIGIN,
                        LONGITUDE_OF_ORIGIN,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertAzimuthalEqualArea() {
        this(PARAMETERS);
    }

    /**
     * Constructs a math transform provider from a set of parameters.
     *
     * @param  parameters  the set of parameters (never {@code null}).
     */
    LambertAzimuthalEqualArea(final ParameterDescriptorGroup parameters) {
        super(PlanarProjection.class, parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected final NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.LambertAzimuthalEqualArea(this, parameters);
    }
}
