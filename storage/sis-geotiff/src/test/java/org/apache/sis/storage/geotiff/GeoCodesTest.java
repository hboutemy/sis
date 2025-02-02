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
package org.apache.sis.storage.geotiff;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Verifies some {@link GeoCodes} values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public final class GeoCodesTest extends TestCase {
    /**
     * Verifies the constants relative to the polar stereographic projection.
     *
     * @throws FactoryException if the operation method is not found.
     */
    @Test
    public void verifyPolarStereographic() throws FactoryException {
        ParameterDescriptorGroup parameters = parameters("Polar Stereographic (Variant B)");
        assertEquals(GeoCodes.PolarStereographic, parseCode(parameters));
        /*
         * Following are testing `GeoKeys` rather than `GeoCodes`,
         * but we do that as an additional consistency check.
         */
        ParameterDescriptorGroup alternative = parameters("Polar Stereographic (Variant A)");
        assertEquals(GeoKeys.StdParallel1, parseCode(parameters .descriptor("Latitude of standard parallel")));
        assertEquals(GeoKeys.NatOriginLat, parseCode(alternative.descriptor("Latitude of natural origin")));
    }

    /**
     * Returns the parameters for the operation method of the given name.
     */
    private static ParameterDescriptorGroup parameters(final String method) throws FactoryException {
        return DefaultCoordinateOperationFactory.provider().getOperationMethod(method).getParameters();
    }

    /**
     * Returns the GeoTIFF code declared in the given object.
     */
    private static int parseCode(final IdentifiedObject object) {
        return Integer.parseInt(IdentifiedObjects.getIdentifier(object, Citations.GEOTIFF).getCode());
    }
}
