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

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.filter.Node;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;

// Branch-dependent imports
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;


/**
 * Expressions that do not depend on any other expression.
 * Those expression may read value from a feature property, or return a constant value.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 * @param  <V>  the type of value computed by the expression.
 *
 * @since 1.1
 * @module
 */
abstract class LeafExpression<R,V> extends Node implements FeatureExpression<R,V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4262341851590811918L;

    /**
     * Creates a new property reader.
     */
    LeafExpression() {
    }

    /**
     * Returns the expression used as parameters for this function,
     * which is an empty list.
     */
    @Override
    public final List<Expression<? super R, ?>> getParameters() {
        return List.of();
    }




    /**
     * A constant, literal value that can be used in expressions.
     * The {@link #apply(Object)} method ignores the argument and always returns {@link #getValue()}.
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <V>  the type of value computed by the expression.
     */
    static class Literal<R,V> extends LeafExpression<R,V> implements org.apache.sis.internal.geoapi.filter.Literal<R,V> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -8383113218490957822L;

        /** The constant value to be returned by {@link #getValue()}. */
        @SuppressWarnings("serial")         // Not statically typed as Serializable.
        protected final V value;

        /** Creates a new literal holding the given constant value. */
        Literal(final V value) {
            this.value = value;             // Null is accepted.
        }

        /** For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations. */
        @Override protected Collection<?> getChildren() {
            // Not `List.of(…)` because value may be null.
            return Collections.singleton(value);
        }

        /** Returns the constant value held by this object. */
        @Override public V getValue() {
            return value;
        }

        /** Returns the type of values computed by this expression. */
        @Override public Class<?> getValueClass() {
            return (value != null) ? value.getClass() : Object.class;
        }

        /** Expression evaluation, which just returns the constant value. */
        @Override public V apply(Object ignored) {
            return value;
        }

        /**
         * Returns an expression that provides values as instances of the specified class.
         *
         * @throws ClassCastException if values cannot be provided as instances of the specified class.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <N> Expression<R,N> toValueType(final Class<N> target) {
            try {
                final N c = ObjectConverters.convert(value, target);
                return (c != value) ? new Literal<>(c) : (Literal<R,N>) this;
            } catch (UnconvertibleObjectException e) {
                throw (ClassCastException) new ClassCastException(Errors.format(
                        Errors.Keys.CanNotConvertValue_2, getFunctionName(), target)).initCause(e);
            }
        }

        /**
         * Provides the type of values returned by {@link #apply(Object)}
         * wrapped in an {@link DefaultAttributeType} named "Literal".
         *
         * @param  addTo  where to add the type of properties evaluated by the given expression.
         * @return builder of the added property.
         */
        @Override
        public PropertyTypeBuilder expectedType(DefaultFeatureType ignored, final FeatureTypeBuilder addTo) {
            final Class<?> valueType = getValueClass();
            DefaultAttributeType<?> propertyType;
            synchronized (TYPES) {
                propertyType = TYPES.get(valueType);
                if (propertyType == null) {
                    final Class<?> standardType = Classes.getStandardType(valueType);
                    propertyType = TYPES.computeIfAbsent(standardType, Literal::newType);
                    if (valueType != standardType) {
                        TYPES.put(valueType, propertyType);
                    }
                }
            }
            return addTo.addProperty(propertyType);
        }

        /**
         * A cache of {@link DefaultAttributeType} instances for literal classes. Used for avoiding to create
         * duplicated instances when the literal is a common type like {@link String} or {@link Integer}.
         */
        @SuppressWarnings("unchecked")
        private static final WeakValueHashMap<Class<?>, DefaultAttributeType<?>> TYPES = new WeakValueHashMap<>((Class) Class.class);

        /**
         * Invoked when a new attribute type need to be created for the given standard type.
         * The given standard type should be a GeoAPI interface, not the implementation class.
         */
        private static <R> DefaultAttributeType<R> newType(final Class<R> standardType) {
            return createType(standardType, Names.createLocalName(null, null, "Literal"));
        }
    }




    /**
     * A literal value which is the result of transforming another literal.
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <V>  the type of value computed by the expression.
     */
    static final class Transformed<R,V> extends Literal<R,V> implements Optimization.OnExpression<R,V> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -5120203649333919221L;

        /** The original expression. */
        @SuppressWarnings("serial")         // Not statically typed as Serializable.
        final Expression<R,?> original;

        /** Creates a new literal holding the given constant value. */
        Transformed(final V value, final Expression<R,?> original) {
            super(value);
            this.original = original;
        }

        /**
         * Returns the same literal without the reference to the original expression.
         * Since this {@code Transformed} instance will not longer be unwrapped,
         * the transformed value will become visible to users.
         */
        @Override
        public Expression<? super R, ? extends V> optimize(final Optimization optimization) {
            return Optimization.literal(getValue());
        }

        /**
         * Converts the transformed value if possible, or the original value as a fallback.
         *
         * @throws ClassCastException if values cannot be provided as instances of the specified class.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <N> Expression<R,N> toValueType(final Class<N> target) {
            // Same implementation than `super.toValueType(type)` except for exception handling.
            try {
                final N c = ObjectConverters.convert(value, target);
                return (c != value) ? new Literal<>(c) : (Literal<R,N>) this;
            } catch (UnconvertibleObjectException e) {
                try {
                    return original.toValueType(target);
                } catch (RuntimeException bis) {
                    final ClassCastException c = new ClassCastException(Errors.format(
                            Errors.Keys.CanNotConvertValue_2, getFunctionName(), target));
                    c.initCause(e);
                    c.addSuppressed(bis);
                    throw c;
                }
            }
        }
    }
}
