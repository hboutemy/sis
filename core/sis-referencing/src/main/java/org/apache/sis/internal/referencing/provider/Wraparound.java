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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;


/**
 * Provider for {@link WraparoundTransform} (SIS-specific operation).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
@XmlTransient
public final class Wraparound extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7464255385789611569L;

    /**
     * The operation parameter descriptor for the number of source and target dimensions.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> dim </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: [1…∞)</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Integer> DIMENSION;

    /**
     * The operation parameter descriptor for the dimension where wraparound is applied.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> SIS:     </td><td> wraparound_dim </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: [0…∞)</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Integer> WRAPAROUND_DIMENSION;

    /**
     * The operation parameter descriptor for for the period.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> SIS:     </td><td> period </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞)</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> PERIOD;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.SIS, "SIS");
        DIMENSION = builder.addName(Molodensky.DIMENSION.getName()).createBounded(Integer.class, 1, null, null);
        WRAPAROUND_DIMENSION = builder.addName("wraparound_dim").createBounded(Integer.class, 0, null, null);
        PERIOD = builder.addName("period").createStrictlyPositive(Double.NaN, null);
        PARAMETERS = builder.addName("Wraparound")
                .createGroup(DIMENSION,
                             WRAPAROUND_DIMENSION,
                             PERIOD);
    }

    /**
     * Constructs a new provider.
     */
    public Wraparound() {
        super(Conversion.class, PARAMETERS,
              CoordinateSystem.class, 2, false,
              CoordinateSystem.class, 2, false);
    }

    /**
     * Creates a wraparound transform from the specified group of parameter values.
     *
     * @param  factory  the factory to use for creating concatenated transforms.
     * @param  values   the group of parameter values.
     * @return the created wraparound transform.
     * @throws FactoryException if a transform cannot be created.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        final Parameters pg = Parameters.castOrWrap(values);
        return WraparoundTransform.create(
                pg.intValue(DIMENSION),
                pg.intValue(WRAPAROUND_DIMENSION),
                pg.doubleValue(PERIOD), Double.NaN, 0);
    }
}
