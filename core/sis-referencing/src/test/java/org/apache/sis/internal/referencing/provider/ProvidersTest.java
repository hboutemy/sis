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

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests some consistency rules of all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.6
 */
@DependsOn({
    org.apache.sis.referencing.operation.DefaultOperationMethodTest.class,
    AffineTest.class,
    LongitudeRotationTest.class,
    MapProjectionTest.class
})
public final class ProvidersTest extends TestCase {
    /**
     * Returns all providers to test.
     */
    private static Class<?>[] methods() {
        return new Class<?>[] {
            Affine.class,
            AxisOrderReversal.class,
            AxisOrderReversal3D.class,
            GeographicOffsets.class,
            GeographicOffsets2D.class,
            GeographicAndVerticalOffsets.class,
            VerticalOffset.class,
            LongitudeRotation.class,
            CoordinateFrameRotation.class,
            CoordinateFrameRotation2D.class,
            CoordinateFrameRotation3D.class,
            PositionVector7Param.class,
            PositionVector7Param2D.class,
            PositionVector7Param3D.class,
            GeocentricTranslation.class,
            GeocentricTranslation2D.class,
            GeocentricTranslation3D.class,
            GeographicToGeocentric.class,
            GeocentricToGeographic.class,
            GeocentricToTopocentric.class,
            GeographicToTopocentric.class,
            Geographic3Dto2D.class,
            Geographic2Dto3D.class,
            Molodensky.class,
            AbridgedMolodensky.class,
            PseudoPlateCarree.class,
            Equirectangular.class,
            Mercator1SP.class,
            Mercator2SP.class,
            MercatorSpherical.class,
            PseudoMercator.class,
            MercatorAuxiliarySphere.class,
            RegionalMercator.class,
            MillerCylindrical.class,
            LambertConformal1SP.class,
            LambertConformal2SP.class,
            LambertConformalWest.class,
            LambertConformalBelgium.class,
            LambertConformalMichigan.class,
            LambertCylindricalEqualArea.class,
            LambertCylindricalEqualAreaSpherical.class,
            LambertAzimuthalEqualArea.class,
            LambertAzimuthalEqualAreaSpherical.class,
            AlbersEqualArea.class,
            TransverseMercator.class,
            TransverseMercatorSouth.class,
            CassiniSoldner.class,
            HyperbolicCassiniSoldner.class,
            PolarStereographicA.class,
            PolarStereographicB.class,
            PolarStereographicC.class,
            PolarStereographicNorth.class,
            PolarStereographicSouth.class,
            ObliqueStereographic.class,
            ObliqueMercator.class,
            ObliqueMercatorCenter.class,
            ObliqueMercatorTwoPoints.class,
            ObliqueMercatorTwoPointsCenter.class,
            Orthographic.class,
            ModifiedAzimuthalEquidistant.class,
            AzimuthalEquidistantSpherical.class,
            ZonedTransverseMercator.class,
            SatelliteTracking.class,
            Sinusoidal.class,
            PseudoSinusoidal.class,
            Polyconic.class,
            Mollweide.class,
            SouthPoleRotation.class,
            NorthPoleRotation.class,
            NTv2.class,
            NTv1.class,
            NADCON.class,
            FranceGeocentricInterpolation.class,
            MolodenskyInterpolation.class,
            Interpolation1D.class,
            Wraparound.class
        };
    }

    /**
     * Returns the subset of {@link #methods()} which are expected to support
     * {@link AbstractProvider#redimension(int, int)}, not including map projections.
     */
    private static Class<?>[] redimensionables() {
        return new Class<?>[] {
            Affine.class,
            LongitudeRotation.class,
            GeographicOffsets.class,
            GeographicOffsets2D.class,
            GeographicAndVerticalOffsets.class,
            CoordinateFrameRotation2D.class,
            CoordinateFrameRotation3D.class,
            PositionVector7Param2D.class,
            PositionVector7Param3D.class,
            GeocentricTranslation2D.class,
            GeocentricTranslation3D.class,
            Geographic3Dto2D.class,
            Geographic2Dto3D.class,
            Molodensky.class,
            AbridgedMolodensky.class,
            FranceGeocentricInterpolation.class
        };
    }

    /**
     * Ensures that every parameter instance is unique. Actually this test is not strong requirement.
     * This is only for sharing existing resources by avoiding unnecessary objects duplication.
     *
     * @throws ReflectiveOperationException if the instantiation of a service provider failed.
     */
    @Test
    public void ensureParameterUniqueness() throws ReflectiveOperationException {
        final Map<GeneralParameterDescriptor, String> groupNames = new IdentityHashMap<>();
        final Map<GeneralParameterDescriptor, GeneralParameterDescriptor> parameters = new HashMap<>();
        final Map<Object, Object> namesAndIdentifiers = new HashMap<>();
        for (final Class<?> c : methods()) {
            final OperationMethod method = (OperationMethod) c.getConstructor((Class[]) null).newInstance((Object[]) null);
            final ParameterDescriptorGroup group = method.getParameters();
            final String operationName = group.getName().getCode();
            for (final GeneralParameterDescriptor param : group.descriptors()) {
                if (operationName.equals(groupNames.put(param, operationName))) {
                    fail("Parameter declared twice in the same “" + operationName + "” group.");
                }
                /*
                 * Ensure uniqueness of the parameter descriptor as a whole.
                 */
                final Identifier name = param.getName();
                Object existing = parameters.put(param, param);
                if (existing != null && existing != param) {
                    fail("Parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                            + " was already defined in “" + groupNames.get(existing) + "”."
                            + " The same instance could be shared.");
                }
                /*
                 * Ensure uniqueness of each name and identifier.
                 */
                existing = namesAndIdentifiers.put(name, name);
                if (existing != null && existing != name) {
                    fail("The name of parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                            + " was already defined elsewhere. The same instance could be shared.");
                }
                for (final GenericName alias : param.getAlias()) {
                    existing = namesAndIdentifiers.put(alias, alias);
                    if (existing != null && existing != alias) {
                        fail("Alias “" + alias + "” of parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                                + " was already defined elsewhere. The same instance could be shared.");
                    }
                }
                for (final Identifier id : param.getIdentifiers()) {
                    existing = namesAndIdentifiers.put(id, id);
                    if (existing != null && existing != id) {
                        fail("Identifier “" + id + "” of parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                                + " was already defined elsewhere. The same instance could be shared.");
                    }
                }
            }
        }
    }

    /** Temporary flag for disabling tests that require JDK9. */
    private static final boolean JDK9 = false;

    /**
     * Tests {@link AbstractProvider#redimension(int, int)} on all providers.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testRedimension() {
        final Map<Class<?>,Boolean> redimensionables = new HashMap<>(100);
        for (final Class<?> type : methods()) {
            assertNull(type.getName(), redimensionables.put(type, Boolean.FALSE));
        }
        for (final Class<?> type : redimensionables()) {
            assertEquals(type.getName(), Boolean.FALSE, redimensionables.put(type, Boolean.TRUE));
        }
        for (final OperationMethod method : ServiceLoader.load(OperationMethod.class)) {
            if (method instanceof ProviderMock) {
                continue;                           // Skip the methods that were defined only for test purpose.
            }
            final AbstractProvider provider = (AbstractProvider) method;
            final Integer sourceDimensions = provider.getSourceDimensions();
            final Integer targetDimensions = provider.getTargetDimensions();
            final Boolean isRedimensionable = redimensionables.get(provider.getClass());
            assertNotNull(provider.getClass().getName(), isRedimensionable);
            if (isRedimensionable) {
                assertNotNull(sourceDimensions);
                assertNotNull(targetDimensions);
                for (int newSource = 2; newSource <= 3; newSource++) {
                    for (int newTarget = 2; newTarget <= 3; newTarget++) {
                        final AbstractProvider redim = provider.redimension(newSource, newTarget);
                        assertEquals("sourceDimensions", newSource, redim.getSourceDimensions().intValue());
                        assertEquals("targetDimensions", newTarget, redim.getTargetDimensions().intValue());
                        if (provider instanceof Affine) {
                            continue;
                        }
                        if (newSource == sourceDimensions && newTarget == targetDimensions) {
                            assertSame("When asking the original number of dimensions, expected the original instance.", provider, redim);
                        } else {
                            assertNotSame("When asking a different number of dimensions, expected a different instance.", provider, redim);
                        }
                        if (JDK9)       // Temporarily disables next line. Will be removed soon.
                        assertSame("When asking the original number of dimensions, expected the original instance.",
                                   provider, redim.redimension(sourceDimensions, targetDimensions));
                    }
                }
            } else if (provider instanceof MapProjection) {
                assertEquals(2, sourceDimensions.intValue());
                assertEquals(2, targetDimensions.intValue());
                final AbstractProvider proj3D = provider.redimension(sourceDimensions ^ 1, targetDimensions ^ 1);
                assertNotSame("redimension(3,3) should return a new method.", provider, proj3D);
                assertSame("redimension(2,2) should give back the original method.", provider,
                        proj3D.redimension(sourceDimensions, targetDimensions));
                assertSame("Value of redimension(3,3) should have been cached.", proj3D,
                        ((MapProjection) provider).redimension(sourceDimensions ^ 1, targetDimensions ^ 1));
            } else if (sourceDimensions != null && targetDimensions != null) try {
                provider.redimension(sourceDimensions + 1, targetDimensions + 1);
                fail("Type " + provider.getClass().getName() + " is not in our list of redimensionable methods.");
            } catch (IllegalArgumentException e) {
                final String message = e.getMessage();
                assertTrue(message, message.contains(provider.getName().getCode()));
            }
        }
    }

    /**
     * Tests the description provided in some parameters.
     */
    @Test
    public void testDescription() {
        assertFalse(((DefaultParameterDescriptor<Double>) SatelliteTracking.SATELLITE_ORBIT_INCLINATION).getDescription().length() == 0);
        assertFalse(((DefaultParameterDescriptor<Double>) SatelliteTracking.SATELLITE_ORBITAL_PERIOD   ).getDescription().length() == 0);
        assertFalse(((DefaultParameterDescriptor<Double>) SatelliteTracking.ASCENDING_NODE_PERIOD      ).getDescription().length() == 0);
    }
}
