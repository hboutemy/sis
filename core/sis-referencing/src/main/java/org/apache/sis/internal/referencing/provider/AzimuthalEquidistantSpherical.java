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
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.PlanarProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for <cite>"Azimuthal Equidistant (Spherical)"</cite> projection.
 * This projection method has no EPSG code.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see <a href="http://geotiff.maptools.org/proj_list/azimuthal_equidistant.html">GeoTIFF parameters for Azimuthal Equidistant</a>
 *
 * @since 1.1
 */
@XmlTransient
public final class AzimuthalEquidistantSpherical extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1152512250113874950L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder().setCodeSpace(null, null)
                .addName("Azimuthal Equidistant (Spherical)")
                .createGroupForMapProjection(
                        ModifiedAzimuthalEquidistant.LATITUDE_OF_ORIGIN,
                        ModifiedAzimuthalEquidistant.LONGITUDE_OF_ORIGIN,
                        ModifiedAzimuthalEquidistant.FALSE_EASTING,
                        ModifiedAzimuthalEquidistant.FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public AzimuthalEquidistantSpherical() {
        super(PlanarProjection.class, PARAMETERS);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected final NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.AzimuthalEquidistant(this, parameters);
    }
}
