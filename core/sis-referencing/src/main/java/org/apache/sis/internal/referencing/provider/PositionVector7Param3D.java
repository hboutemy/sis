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

import java.util.Arrays;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for <cite>"Position Vector transformation (geog3D domain)"</cite> (EPSG:1037).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.7
 */
@XmlTransient
public final class PositionVector7Param3D extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8909802769265229915L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("1037")
                .addName("Position Vector transformation (geog3D domain)")
                .createGroupWithSameParameters(PositionVector7Param2D.PARAMETERS);
        /*
         * NOTE: we omit the "Bursa-Wolf" alias because it is ambiguous, since it can apply
         * to both "Coordinate Frame rotation" and "Position Vector 7-param. transformation"
         * We also omit "Position Vector 7-param. transformation" alias for similar reason.
         */
    }

    /**
     * The providers for all combinations between 2D and 3D cases.
     */
    static final GeocentricAffineBetweenGeographic[] REDIMENSIONED = new GeocentricAffineBetweenGeographic[4];
    static {
        Arrays.setAll(REDIMENSIONED, (i) -> (i == INDEX_OF_2D)
                ? new PositionVector7Param2D(i)
                : new PositionVector7Param3D(i));
    }

    /**
     * Returns the provider for the specified combination of source and target dimensions.
     */
    @Override
    final GeodeticOperation redimensioned(int indexOfDim) {
        return REDIMENSIONED[indexOfDim];
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    public PositionVector7Param3D() {
        super(REDIMENSIONED[INDEX_OF_3D]);
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param indexOfDim  number of dimensions as the index in {@code redimensioned} array.
     */
    private PositionVector7Param3D(int indexOfDim) {
        super(Type.SEVEN_PARAM, PARAMETERS, indexOfDim);
    }
}
