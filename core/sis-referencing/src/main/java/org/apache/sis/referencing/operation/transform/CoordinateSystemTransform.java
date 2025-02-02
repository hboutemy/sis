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
package org.apache.sis.referencing.operation.transform;

import java.util.Map;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.DefaultOperationMethod;


/**
 * Base class of conversions between coordinate systems.
 * Each subclass should have a singleton instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.7
 */
abstract class CoordinateSystemTransform extends AbstractMathTransform {
    /**
     * Number of input and output dimensions.
     */
    private final int dimension;

    /**
     * An operation method that describe this coordinate system conversion.
     * This is used for providing a value in {@link DefaultMathTransformFactory#getLastMethodUsed()}.
     */
    private final transient OperationMethod method;

    /**
     * The {@linkplain #method} augmented with one pass through dimension.
     * May be the same instance than {@link #method} if that method is already 3D.
     *
     * <div class="note"><b>Note:</b> if {@link #method} is "Polar to Cartesian",
     * then {@code method3D} is "Cylindrical to Cartesian".</div>
     *
     * This method is used for {@link org.opengis.referencing.operation.CoordinateOperation} WKT formatting.
     * Contrarily to {@link #method}, this {@code method3D} is never used for {@link MathTransform} WKT.
     * Instead, the latter case is represented by a concatenation of {@link #method} with a pass-through.
     */
    private final transient OperationMethod method3D;

    /**
     * An empty contextual parameter, used only for representing conversion from degrees to radians.
     */
    final transient ContextualParameters context;

    /**
     * The complete transform, including conversion between degrees and radians.
     *
     * @see #completeTransform(MathTransformFactory)
     */
    private transient volatile MathTransform complete;

    /**
     * The {@link #complete} transform in a {@link PassThroughTransform} with a 1 trailing coordinate.
     * This is used for supporting the cylindrical case on top the polar case.
     *
     * @see #passthrough(MathTransformFactory)
     */
    private transient volatile MathTransform passthrough;

    /**
     * Creates a new conversion between two types of coordinate system.
     * Subclasses may need to invoke {@link ContextualParameters#normalizeGeographicInputs(double)}
     * or {@link ContextualParameters#denormalizeGeographicOutputs(double)} after this constructor.
     */
    CoordinateSystemTransform(final String method, final String method3D, final int dimension) {
        this.dimension = dimension;
        this.method    = method(method);
        this.method3D  = (method3D != null) ? method(method3D) : this.method;
        this.context   = new ContextualParameters(this.method.getParameters(), dimension, dimension);
    }

    /**
     * Creates an operation method of the given name.
     */
    private static OperationMethod method(final String name) {
        final Map<String,?> properties = Map.of(DefaultParameterDescriptorGroup.NAME_KEY,
                new ImmutableIdentifier(Citations.SIS, Constants.SIS, name));
        final DefaultParameterDescriptorGroup descriptor = new DefaultParameterDescriptorGroup(properties, 1, 1);
        return new DefaultOperationMethod(properties, descriptor);
    }

    /**
     * Returns the complete transform, including conversion between degrees and radians units.
     */
    final MathTransform completeTransform(final MathTransformFactory factory) throws FactoryException {
        MathTransform tr = complete;
        if (tr == null) {
            tr = context.completeTransform(factory, this);
            if (CoordinateOperations.isDefaultInstance(factory)) {
                // No need to synchronize since DefaultMathTransformFactory returns unique instances.
                complete = tr;
            }
        }
        return tr;
    }

    /**
     * Returns the cylindrical, including conversion between degrees and radians units.
     * This method is legal only for {@link PolarToCartesian} or {@link CartesianToPolar}.
     */
    final MathTransform passthrough(final MathTransformFactory factory) throws FactoryException {
        MathTransform tr = passthrough;
        if (tr == null) {
            tr = factory.createPassThroughTransform(0, completeTransform(factory), 1);
            if (CoordinateOperations.isDefaultInstance(factory)) {
                // No need to synchronize since DefaultMathTransformFactory returns unique instances.
                passthrough = tr;
            }
        }
        return tr;
    }

    /**
     * Returns the number of dimensions in the source coordinate tuples.
     * Shall be equal to {@code getSourceCS().getDimension()}.
     */
    @Override
    public final int getSourceDimensions() {
        return dimension;
    }

    /**
     * Returns the number of dimensions in the target coordinate tuples.
     * Shall be equal to {@code getTargetCS().getDimension()}.
     */
    @Override
    public final int getTargetDimensions() {
        return dimension;
    }

    /**
     * Returns the empty set of parameter values.
     */
    @Override
    public final ParameterValueGroup getParameterValues() {
        return context;
    }

    /**
     * Returns the contextual parameters. This is used for telling to the Well Known Text (WKT) formatter that this
     * {@code CoordinateSystemTransform} transform is usually preceeded or followed by a conversion between degree
     * and radian units of measurement.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Implementation of {@link DefaultMathTransformFactory#createCoordinateSystemChange(CoordinateSystem,
     * CoordinateSystem, Ellipsoid)}, defined here for reducing the {@code DefaultMathTransformFactory}
     * weight in the common case where the conversions handled by this class are not needed.
     */
    static MathTransform create(final MathTransformFactory factory, final CoordinateSystem source,
            final CoordinateSystem target, final ThreadLocal<OperationMethod> lastMethod) throws FactoryException
    {
        int passthrough = 0;
        CoordinateSystemTransform kernel = null;
        if (source instanceof CartesianCS) {
            if (target instanceof SphericalCS) {
                kernel = CartesianToSpherical.INSTANCE;
            } else if (target instanceof PolarCS) {
                kernel = CartesianToPolar.INSTANCE;
            } else if (target instanceof CylindricalCS) {
                kernel = CartesianToPolar.INSTANCE;
                passthrough = 1;
            }
        } else if (target instanceof CartesianCS) {
            if (source instanceof SphericalCS) {
                kernel = SphericalToCartesian.INSTANCE;
            } else if (source instanceof PolarCS) {
                kernel = PolarToCartesian.INSTANCE;
            } else if (source instanceof CylindricalCS) {
                kernel = PolarToCartesian.INSTANCE;
                passthrough = 1;
            }
        }
        Exception cause = null;
        try {
            if (kernel == null) {
                return factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(source, target));
            } else if (source.getDimension() == kernel.getSourceDimensions() + passthrough &&
                       target.getDimension() == kernel.getTargetDimensions() + passthrough)
            {
                final MathTransform tr = (passthrough == 0)
                        ? kernel.completeTransform(factory)
                        : kernel.passthrough(factory);
                final MathTransform before = factory.createAffineTransform(
                        CoordinateSystems.swapAndScaleAxes(source,
                        CoordinateSystems.replaceAxes(source, AxesConvention.NORMALIZED)));
                final MathTransform after  = factory.createAffineTransform(
                        CoordinateSystems.swapAndScaleAxes(
                        CoordinateSystems.replaceAxes(target, AxesConvention.NORMALIZED), target));
                final MathTransform result = factory.createConcatenatedTransform(before,
                                             factory.createConcatenatedTransform(tr, after));
                lastMethod.set(passthrough == 0 ? kernel.method : kernel.method3D);
                return result;
            }
        } catch (IllegalArgumentException | IncommensurableException e) {
            cause = e;
        }
        throw new OperationNotFoundException(Resources.format(Resources.Keys.CoordinateOperationNotFound_2,
                WKTUtilities.toType(CoordinateSystem.class, source.getClass()),
                WKTUtilities.toType(CoordinateSystem.class, target.getClass())), cause);
    }
}
