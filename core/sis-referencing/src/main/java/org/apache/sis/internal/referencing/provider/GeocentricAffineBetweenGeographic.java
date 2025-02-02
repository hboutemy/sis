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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Units;


/**
 * The base class of operation methods performing an affine operation in geocentric coordinates
 * concatenated with conversion from/to geographic coordinates. This base class is also used for
 * operation methods performing <em>approximation</em> of above, even if they do not really pass
 * through geocentric coordinates.
 *
 * <h2>Default values to verify</h2>
 * This class assumes the following default values.
 * Subclasses should verify if those default values are suitable from them:
 *
 * <ul>
 *   <li>{@link #getOperationType()} defaults to {@link org.opengis.referencing.operation.Transformation}.</li>
 *   <li>{@link #sourceCSType} and {@link #targetCSType} default to {@link EllipsoidalCS}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.7
 */
@XmlTransient
public abstract class GeocentricAffineBetweenGeographic extends GeocentricAffine {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6202315859507526222L;

    /**
     * The operation parameter descriptor for the number of source and target geographic dimensions (2 or 3).
     * This is an OGC-specific parameter for the {@link Molodensky} and {@link AbridgedMolodensky} operations,
     * but Apache SIS uses it also for internal parameters of Geographic/Geocentric.
     *
     * <p>We do not provide default value for this parameter (neither we do for other OGC-specific parameters
     * in this class) because this parameter is used with both two- and three-dimensional operation methods.
     * If we want to provide a default value, we could but it would complicate a little bit the code since we
     * could no longer reuse the same {@code PARAMETERS} constant for operation methods of any number of dimensions.</p>
     *
     * @see GeographicToGeocentric#DIMENSION
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> dim </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: [2…3]</li>
     *   <li>No default value</li>
     *   <li>Optional</li>
     * </ul>
     */
    public static final ParameterDescriptor<Integer> DIMENSION;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> src_semi_major </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> SRC_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> src_semi_minor </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> SRC_SEMI_MINOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> tgt_semi_major </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> TGT_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> tgt_semi_minor </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> TGT_SEMI_MINOR;

    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.OGC, Constants.OGC);
        SRC_SEMI_MAJOR = builder.addName("src_semi_major").createStrictlyPositive(Double.NaN, Units.METRE);
        SRC_SEMI_MINOR = builder.addName("src_semi_minor").createStrictlyPositive(Double.NaN, Units.METRE);
        TGT_SEMI_MAJOR = builder.addName("tgt_semi_major").createStrictlyPositive(Double.NaN, Units.METRE);
        TGT_SEMI_MINOR = builder.addName("tgt_semi_minor").createStrictlyPositive(Double.NaN, Units.METRE);
        DIMENSION      = builder.addName(Constants.DIM).setRequired(false).createBounded(Integer.class, 2, 3, null);
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    GeocentricAffineBetweenGeographic(final GeocentricAffineBetweenGeographic copy) {
        super(copy);
    }

    /**
     * Constructs a provider with the specified parameters.
     *
     * @param type        the operation type as an enumeration value.
     * @param parameters  description of parameters expected by this operation.
     * @param indexOfDim  number of dimensions as the index in {@link #redimensioned} array.
     */
    GeocentricAffineBetweenGeographic(Type operationType, ParameterDescriptorGroup parameters, int indexOfDim) {
        super(operationType, parameters, indexOfDim,
              EllipsoidalCS.class, true,
              EllipsoidalCS.class, true);
    }

    /**
     * Creates a math transform from the specified group of parameter values.
     * This method wraps the affine operation into Geographic/Geocentric conversions.
     *
     * @param  factory  the factory to use for creating concatenated transforms.
     * @param  values   the group of parameter values.
     * @return the created math transform.
     * @throws FactoryException if a transform cannot be created.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        final Parameters pv = Parameters.castOrWrap(values);
        final MathTransform affine = super.createMathTransform(factory, pv);
        /*
         * Create a "Geographic to Geocentric" conversion with ellipsoid axis length units converted to metres
         * (the unit implied by SRC_SEMI_MAJOR) because it is the unit of Bursa-Wolf parameters that we created above.
         */
        MathTransform toGeocentric = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                pv.doubleValue(SRC_SEMI_MAJOR),
                pv.doubleValue(SRC_SEMI_MINOR),
                Units.METRE, getSourceDimensions() >= 3,
                EllipsoidToCentricTransform.TargetType.CARTESIAN);
        /*
         * Create a "Geocentric to Geographic" conversion with ellipsoid axis length units converted to metres
         * because this is the unit of the Geocentric CRS used above.
         */
        MathTransform toGeographic = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                pv.doubleValue(TGT_SEMI_MAJOR),
                pv.doubleValue(TGT_SEMI_MINOR),
                Units.METRE, getTargetDimensions() >= 3,
                EllipsoidToCentricTransform.TargetType.CARTESIAN);
        try {
            toGeographic = toGeographic.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen with SIS implementation.
        }
        /*
         * The  Geocentric → Affine → Geographic  chain.
         */
        return factory.createConcatenatedTransform(toGeocentric,
               factory.createConcatenatedTransform(affine, toGeographic));
    }
}
