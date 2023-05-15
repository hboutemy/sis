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
package org.apache.sis.filter;

import java.util.Set;
import java.util.List;
import java.util.Collection;
import org.opengis.util.ScopedName;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.filter.Expression;
import org.opengis.feature.FeatureType;


/**
 * Expression whose results are converted to a different type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <S>  the type of value computed by the wrapped exception. This is the type to convert.
 * @param  <V>  the type of value computed by this expression. This is the type after conversion.
 *
 * @see org.apache.sis.internal.filter.GeometryConverter
 *
 * @since 1.1
 */
final class ConvertFunction<R,S,V> extends UnaryFunction<R,S>
        implements FeatureExpression<R,V>, Optimization.OnExpression<R,V>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4686604324414717316L;

    /**
     * Name of this expression.
     */
    private static final ScopedName NAME = createName("Convert");

    /**
     * The converter to use.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final ObjectConverter<? super S, ? extends V> converter;

    /**
     * Creates a new converted expression.
     *
     * @param  expression  the expression providing source values.
     * @param  source      the type of value produced by given expression
     * @param  target      the desired type for the expression result.
     * @throws UnconvertibleObjectException if no converter is found.
     */
    ConvertFunction(final Expression<R, ? extends S> expression, final Class<S> source, final Class<V> target) {
        super(expression);
        converter = ObjectConverters.find(source, target);
    }

    /**
     * Creates a new converted expression after optimization.
     *
     * @param  expression  the expression providing source values.
     * @throws UnconvertibleObjectException if no converter is found.
     */
    private ConvertFunction(final ConvertFunction<R,S,V> original, final Expression<R, ? extends S> expression) {
        super(expression);
        converter = original.converter;
    }

    /**
     * Creates a new expression of the same type than this expression, but with optimized parameters.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<R,V> recreate(Expression<R,?>[] effective) {
        final Expression<R,?> e = effective[0];
        if (e instanceof FeatureExpression<?,?>) {
            final Class<? extends V> target = getValueClass();                          // This is <V>.
            final Class<?> source = ((FeatureExpression<?,?>) e).getValueClass();       // May become <S>.
            if (target.isAssignableFrom(source)) {
                return (Expression<R,V>) e;
            }
            if (source != Object.class) {
                return new ConvertFunction(e, source, target);
            }
        }
        final Class<? super S> source = converter.getSourceClass();
        return new ConvertFunction(this, e.toValueType(source));
    }

    /**
     * Returns an identification of this operation.
     */
    @Override
    public ScopedName getFunctionName() {
        return NAME;
    }

    /**
     * Returns the manner in which values are computed from given resources.
     * This expression can be represented as the concatenation of the user-supplied expression with the converter.
     * Because this {@code ConvertFunction} does nothing on its own, it does not have its own set of properties.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return FunctionProperty.concatenate(properties(expression), converter.properties());
    }

    /**
     * Returns the singleton expression tested by this operator
     * together with the source and target classes.
     */
    @Override
    protected Collection<?> getChildren() {
        return List.of(expression, converter.getSourceClass(), converter.getTargetClass());
    }

    /**
     * Evaluates the expression for producing a result of the given type.
     * If this method cannot produce a value of the given type, then it returns {@code null}.
     * This implementation evaluates the expression {@linkplain Expression#apply(Object) in the default way},
     * then tries to convert the result to the target type.
     *
     * @param  feature  the value or feature to evaluate with this expression.
     * @return the result, or {@code null} if it cannot be of the specified type.
     */
    @Override
    public V apply(final R feature) {
        final S value = expression.apply(feature);
        try {
            return converter.apply(value);
        } catch (UnconvertibleObjectException e) {
            warning(e, false);
            return null;
        }
    }

    /**
     * Returns the type of values computed by this expression.
     */
    @Override
    public Class<? extends V> getValueClass() {
        return converter.getTargetClass();
    }

    /**
     * Provides the type of values produced by this expression when a feature of the given type is evaluated.
     * May return {@code null} if the type cannot be determined.
     */
    @Override
    public PropertyTypeBuilder expectedType(final FeatureType valueType, final FeatureTypeBuilder addTo) {
        final FeatureExpression<?,?> fex = FeatureExpression.castOrCopy(expression);
        if (fex == null) {
            return null;
        }
        final PropertyTypeBuilder p = fex.expectedType(valueType, addTo);
        if (p instanceof AttributeTypeBuilder<?>) {
            return ((AttributeTypeBuilder<?>) p).setValueClass(getValueClass());
        }
        return p;
    }

    /**
     * Returns an expression doing the same evaluation than this method, but returning results as values
     * of the specified type. The result may be {@code this}.
     *
     * @param  <N>     compile-time value of {@code type}.
     * @param  target  desired type of expression results.
     * @return expression doing the same operation this this expression but with results of the specified type.
     * @throws ClassCastException if the specified type is not a target type supported by implementation.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <N> Expression<R,N> toValueType(final Class<N> target) {
        if (target.isAssignableFrom(getValueClass())) {
            return (Expression<R,N>) this;
        }
        final Class<? super S> source = converter.getSourceClass();
        if (target.isAssignableFrom(source)) {
            return (Expression<R,N>) expression;
        } else try {
            return new ConvertFunction<>(expression, source, target);
        } catch (UnconvertibleObjectException e) {
            throw (ClassCastException) new ClassCastException(Errors.format(
                    Errors.Keys.CanNotConvertValue_2, expression.getFunctionName(), target)).initCause(e);
        }
    }
}
