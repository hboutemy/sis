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

import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Units;
import org.apache.sis.internal.util.Constants;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;


/**
 * The provider for the <cite>"Geocentric/topocentric conversions"</cite> (EPSG:9836).
 * This operation is implemented using existing {@link MathTransform} implementations;
 * there is no need for a class specifically for this transform.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class GeocentricToTopocentric extends AbstractProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6064563343153407987L;

    /**
     * The operation parameter descriptor for the <cite>Geocentric X of topocentric origin</cite> (X) parameter value.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     */
    private static final ParameterDescriptor<Double> ORIGIN_X;

    /**
     * The operation parameter descriptor for the <cite>Geocentric Y of topocentric origin</cite> (Y) parameter value.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     */
    private static final ParameterDescriptor<Double> ORIGIN_Y;

    /**
     * The operation parameter descriptor for the <cite>Geocentric Z of topocentric origin</cite> (Z) parameter value.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     */
    private static final ParameterDescriptor<Double> ORIGIN_Z;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        ORIGIN_X = builder
                .addIdentifier("8837")
                .addName("Geocentric X of topocentric origin")
                .create(Double.NaN, Units.METRE);

        ORIGIN_Y = builder
                .addIdentifier("8838")
                .addName("Geocentric Y of topocentric origin")
                .create(Double.NaN, Units.METRE);

        ORIGIN_Z = builder
                .addIdentifier("8839")
                .addName("Geocentric Z of topocentric origin")
                .create(Double.NaN, Units.METRE);

        PARAMETERS = builder
                .addIdentifier("9836")
                .addName("Geocentric/topocentric conversions")
                .createGroupForMapProjection(ORIGIN_X, ORIGIN_Y, ORIGIN_Z);
                // Not really a map projection, but we leverage the same axis parameters.
    }

    /**
     * Constructs a provider for the 3-dimensional case.
     */
    public GeocentricToTopocentric() {
        super(3, 3, PARAMETERS);
    }

    /**
     * Returns the operation type.
     *
     * @return {@code Conversion.class}.
     */
    @Override
    public Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Notifies {@code DefaultMathTransformFactory} that Geocentric/topocentric conversions
     * require values for the {@code "semi_major"} and {@code "semi_minor"} parameters.
     *
     * @return 1, meaning that the operation requires a source ellipsoid.
     */
    @Override
    public int getEllipsoidsMask() {
        return 1;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The unit of measurement of input coordinates will be the units of the ellipsoid axes.
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  values   the parameter values that define the transform to create.
     * @return the conversion from geocentric to topocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        try {
            return create(factory, Parameters.castOrWrap(values), false);
        } catch (TransformException e) {
            throw new FactoryException(e);
        }
    }

    /**
     * Implementation of {@link #createMathTransform(MathTransformFactory, ParameterValueGroup)}
     * shared with {@link GeographicToTopocentric}.
     *
     * @param  factory     the factory to use for creating the transform.
     * @param  values      the parameter values that define the transform to create.
     * @param  geographic  {@code true} if the source coordinates are geographic, or
     *                     {@code false} if the source coordinates are geocentric.
     */
    static MathTransform create(final MathTransformFactory factory, final Parameters values, final boolean geographic)
            throws FactoryException, TransformException
    {
        final ParameterValue<?> ap = values.parameter(Constants.SEMI_MAJOR);
        final Unit<Length> unit = ap.getUnit().asType(Length.class);
        final double a = ap.doubleValue();
        final double b = values.parameter(Constants.SEMI_MINOR).doubleValue(unit);
        final double x, y, z, λ, φ;
        final MathTransform toGeocentric;
        if (geographic) {
            /*
             * Full conversion from (longitude, latitude, height) in degrees
             * to geocentric coordinates in linear units (usually metres).
             */
            toGeocentric = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                    a, b, unit, true, EllipsoidToCentricTransform.TargetType.CARTESIAN);

            final double[] origin = new double[] {
                values.doubleValue(GeographicToTopocentric.ORIGIN_X),
                values.doubleValue(GeographicToTopocentric.ORIGIN_Y),
                values.doubleValue(GeographicToTopocentric.ORIGIN_Z)};

            λ = toRadians(origin[0]);
            φ = toRadians(origin[1]);
            toGeocentric.transform(origin, 0, origin, 0, 1);
            x = origin[0];
            y = origin[1];
            z = origin[2];
        } else {
            /*
             * Shorter conversion from (longitude, latitude) in radians to
             * geocentric coordinates as fractions of semi-major axis length.
             * This conversion is used only in this block and is not kept.
             */
            toGeocentric = new EllipsoidToCentricTransform(
                    a, b, unit, false, EllipsoidToCentricTransform.TargetType.CARTESIAN);

            final double[] origin = new double[] {
                (x = values.doubleValue(ORIGIN_X, unit)) / a,
                (y = values.doubleValue(ORIGIN_Y, unit)) / a,
                (z = values.doubleValue(ORIGIN_Z, unit)) / a};

            toGeocentric.inverse().transform(origin, 0, origin, 0, 1);
            λ = origin[0];         // Already in radians.
            φ = origin[1];
        }
        final double sinλ = sin(λ);
        final double cosλ = cos(λ);
        final double sinφ = sin(φ);
        final double cosφ = cos(φ);
        /*
         * Following transform uses the inverse of the matrix R given in EPSG guidance note
         * because it allows us to put the (x,y,z) translation terms directly in the matrix.
         */
        MathTransform mt = factory.createAffineTransform(new Matrix4(
                -sinλ,  -sinφ*cosλ,  cosφ*cosλ,  x,
                 cosλ,  -sinφ*sinλ,  cosφ*sinλ,  y,
                    0,   cosφ,       sinφ,       z,
                    0,   0,          0,          1)).inverse();
        if (geographic) {
            mt = factory.createConcatenatedTransform(toGeocentric, mt);
        }
        return mt;
    }
}
