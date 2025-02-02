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
package org.apache.sis.internal.filter.sqlmm;

import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.filter.Optimization;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;

// Branch-dependent imports
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.filter.Expression;
import org.apache.sis.internal.geoapi.filter.Literal;
import org.apache.sis.internal.geoapi.filter.ValueReference;


/**
 * SQLMM spatial functions taking two geometry operands.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 *
 * @since 1.1
 */
class TwoGeometries<R> extends SpatialFunction<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5209470632171445234L;

    /**
     * The expression giving the geometries.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final Expression<R, GeometryWrapper> geometry1, geometry2;

    /**
     * Creates a new function for geometries represented by the given parameter.
     */
    TwoGeometries(final SQLMM operation, final Expression<R,?>[] parameters, final Geometries<?> library) {
        super(operation, parameters);
        geometry1 = toGeometryWrapper(library, parameters[0]);
        geometry2 = toGeometryWrapper(library, parameters[1]);
    }

    /**
     * Creates a new expression of the same type than this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<R,?>[] effective) {
        return new TwoGeometries<>(operation, effective, getGeometryLibrary());
    }

    /**
     * If the CRS of the first argument is known in advance and the second argument is a literal,
     * transforms the second geometry to the CRS of the first argument. The transformed geometry
     * is always the second argument because according SQLMM specification, operations shall be
     * executed in the CRS of the first argument.
     */
    @Override
    public Expression<R,?> optimize(final Optimization optimization) {
        final DefaultFeatureType featureType = optimization.getFeatureType();
        if (featureType != null) {
            final Expression<R,?> p1 = unwrap(geometry1);
            if (p1 instanceof ValueReference<?,?> && unwrap(geometry2) instanceof Literal<?,?>) try {
                final CoordinateReferenceSystem targetCRS = AttributeConvention.getCRSCharacteristic(
                        featureType, featureType.getProperty(((ValueReference<?,?>) p1).getXPath()));
                if (targetCRS != null) {
                    final GeometryWrapper literal = geometry2.apply(null);
                    if (literal != null) {
                        final GeometryWrapper tr = literal.transform(targetCRS);
                        if (tr != literal) {
                            @SuppressWarnings({"unchecked","rawtypes"})
                            final Expression<R,?>[] effective = getParameters().toArray(Expression[]::new);
                            effective[1] = Optimization.literal(tr);
                            return recreate(effective);
                        }
                    }
                }
            } catch (IllegalArgumentException | TransformException e) {
                warning(e, true);
            }
        }
        return super.optimize(optimization);
    }

    /**
     * Returns a handler for the library of geometric objects used by this expression.
     */
    @Override
    final Geometries<?> getGeometryLibrary() {
        return getGeometryLibrary(geometry1);
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public Class<? super R> getResourceClass() {
        return specializedClass(geometry1.getResourceClass(), geometry2.getResourceClass());
    }

    /**
     * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return List.of(unwrap(geometry1), unwrap(geometry2));
    }

    /**
     * Evaluates the two first expressions as geometry objects,
     * applies the operation and returns the result.
     */
    @Override
    public Object apply(R input) {
        final GeometryWrapper value = geometry1.apply(input);
        if (value != null) {
            final GeometryWrapper other = geometry2.apply(input);
            if (other != null) try {
                return value.operation(operation, other);
            } catch (TransformException | RuntimeException e) {
                warning(e, false);
            }
        }
        return null;
    }


    /**
     * SQLMM spatial functions taking a single geometry operand with one argument.
     */
    static final class WithArgument<R> extends TwoGeometries<R> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -121819663224041806L;

        /**
         * The first argument after the geometries.
         */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        final Expression<R,?> argument;

        /**
         * Creates a new function for geometries represented by the given parameter.
         */
        WithArgument(final SQLMM operation, final Expression<R,?>[] parameters, final Geometries<?> library) {
            super(operation, parameters, library);
            argument = parameters[2];
        }

        /**
         * Creates a new expression of the same type than this expression, but with an optimized geometry.
         * The optimization may be a geometry computed immediately if all operator parameters are literals.
         */
        @Override
        public Expression<R,Object> recreate(final Expression<R,?>[] effective) {
            return new WithArgument<>(operation, effective, getGeometryLibrary());
        }

        /**
         * Returns the class of resources expected by this expression.
         */
        @Override
        public Class<? super R> getResourceClass() {
            return specializedClass(super.getResourceClass(), argument.getResourceClass());
        }

        /**
         * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
         */
        @Override
        public List<Expression<R,?>> getParameters() {
            return List.of(unwrap(geometry1), unwrap(geometry2), argument);
        }

        /**
         * Evaluates the two first expressions as geometry objects,
         * applies the operation and returns the result.
         */
        @Override
        public Object apply(R input) {
            final GeometryWrapper value = geometry1.apply(input);
            if (value != null) {
                final GeometryWrapper other = geometry2.apply(input);
                if (other != null) try {
                    return value.operationWithArgument(operation, other, argument.apply(input));
                } catch (TransformException | RuntimeException e) {
                    warning(e, false);
                }
            }
            return null;
        }
    }
}
