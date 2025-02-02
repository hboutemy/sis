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
package org.apache.sis.feature;

import org.apache.sis.util.resources.Errors;


/**
 * Base class of attributes that are the result of a feature operation.
 * This base class is defined for making easier to identify where computations are done.
 *
 * @todo A future version may provide caching services, methods for taking a snapshot, <i>etc.</i>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param <V> the type of attribute values.
 *
 * @since 1.4
 */
abstract class OperationResult<V> extends AbstractAttribute<V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1418917854672134381L;

    /**
     * The feature instance to use as a source for computing the result.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final AbstractFeature feature;

    /**
     * Creates a new operation for a result of the given type.
     *
     * @param type  information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    protected OperationResult(final DefaultAttributeType<V> type, final AbstractFeature feature) {
        super(type);
        this.feature = feature;
    }

    /**
     * Retro-propagate an operation result to the properties in the source feature instance.
     * This is an optional operation.
     * The default implementation unconditionally throws an {@link UnsupportedOperationException}.
     */
    @Override
    public void setValue(V value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, AbstractAttribute.class));
    }
}
