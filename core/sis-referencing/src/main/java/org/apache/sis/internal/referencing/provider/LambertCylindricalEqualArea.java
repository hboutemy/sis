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

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.referencing.operation.projection.CylindricalEqualArea;


/**
 * The provider for <cite>"Lambert Cylindrical Equal Area"</cite> projection (EPSG:9835).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see <a href="http://geotiff.maptools.org/proj_list/cylindrical_equal_area.html">GeoTIFF parameters for Cylindrical Equal Area</a>
 *
 * @since 0.8
 */
@XmlTransient
public final class LambertCylindricalEqualArea extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -672278344635217838L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9835";

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard parallel</cite> (φ₁) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Latitude of 1st standard parallel </td></tr>
     *   <tr><td> OGC:     </td><td> standard_parallel_1 </td></tr>
     *   <tr><td> ESRI:    </td><td> Standard_Parallel_1 </td></tr>
     *   <tr><td> NetCDF:  </td><td> standard_parallel </td></tr>
     *   <tr><td> GeoTIFF: </td><td> StdParallel1 </td></tr>
     *   <tr><td> Proj4:   </td><td> lat_ts </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (-90.0 … 90.0)°</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL = Equirectangular.STANDARD_PARALLEL;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Longitude of natural origin </td></tr>
     *   <tr><td> OGC:     </td><td> central_meridian </td></tr>
     *   <tr><td> ESRI:    </td><td> Central_Meridian </td></tr>
     *   <tr><td> NetCDF:  </td><td> longitude_of_projection_origin </td></tr>
     *   <tr><td> GeoTIFF: </td><td> NatOriginLong </td></tr>
     *   <tr><td> Proj4:   </td><td> lon_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN = Mercator1SP.LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1. This is not formally a parameter of this projection.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> scale_factor </td></tr>
     *   <tr><td> ESRI:    </td><td> Scale_Factor </td></tr>
     *   <tr><td> Proj4:   </td><td> k </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Optional</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR = Mercator2SP.SCALE_FACTOR;

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
        PARAMETERS = builder()
                .addIdentifier(             IDENTIFIER)
                .addName(                   "Lambert Cylindrical Equal Area")
                .addName(Citations.OGC,     "Cylindrical_Equal_Area")
                .addName(Citations.ESRI,    "Cylindrical_Equal_Area")
                .addName(Citations.GEOTIFF, "CT_CylindricalEqualArea")
                .addName(Citations.PROJ4,   "cea")
                .addIdentifier(Citations.GEOTIFF, "28")
                .createGroupForMapProjection(
                        STANDARD_PARALLEL,
                        LONGITUDE_OF_ORIGIN,
                        SCALE_FACTOR,               // Not formally a Cylindrical Equal Area parameter.
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertCylindricalEqualArea() {
        super(CylindricalProjection.class, PARAMETERS);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new CylindricalEqualArea(this, parameters);
    }
}
