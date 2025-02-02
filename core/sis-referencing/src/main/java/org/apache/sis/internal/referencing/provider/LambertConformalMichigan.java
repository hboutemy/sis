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
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.Units;


/**
 * The provider for <cite>"Lambert Conic Conformal (2SP Michigan)"</cite> projection (EPSG:1051).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.6
 */
@XmlTransient
public final class LambertConformalMichigan extends AbstractLambert {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 877467775093107902L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "1051";

    /**
     * The operation parameter descriptor for the <cite>Ellipsoid scaling factor</cite> (EPSG:1038)
     * parameter value. Valid values range is (0 … ∞) and there is no default value.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Ellipsoid scaling factor </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        SCALE_FACTOR = builder
                .addIdentifier("1051")
                .addName("Ellipsoid scaling factor")
                .createStrictlyPositive(Double.NaN, Units.UNITY);

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName("Lambert Conic Conformal (2SP Michigan)")
                .createGroupForMapProjection(
                        LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN,
                        LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN,
                        LambertConformal2SP.STANDARD_PARALLEL_1,
                        LambertConformal2SP.STANDARD_PARALLEL_2,
                        LambertConformal2SP.EASTING_AT_FALSE_ORIGIN,
                        LambertConformal2SP.NORTHING_AT_FALSE_ORIGIN,
                        SCALE_FACTOR);
    }

    /**
     * Constructs a new provider.
     */
    public LambertConformalMichigan() {
        super(PARAMETERS);
    }
}
