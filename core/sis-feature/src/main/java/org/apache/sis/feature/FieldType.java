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

import java.util.Map;
import java.util.Iterator;
import org.opengis.util.GenericName;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of property types having a value and a multiplicity.
 * This include {@code AttributeType} and {@code AssociationRole}, but not {@code Operation}.
 *
 * <div class="note"><b>Analogy:</b> if we compare {@code FeatureType} to a class in the Java language,
 * attributes and associations would be fields while operations would be methods. This analogy explains
 * the {@code FieldType} name of this class.</div>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.5
 */
abstract class FieldType extends AbstractIdentifiedType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1681767054884098122L;

    /**
     * The minimum number of occurrences of the property within its containing entity.
     */
    private final int minimumOccurs;

    /**
     * The maximum number of occurrences of the property within its containing entity,
     * or {@link Integer#MAX_VALUE} if there is no limit.
     */
    private final int maximumOccurs;

    /**
     * Constructs a field type from the given properties. The identification map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     *
     * @param identification  the name and other information to be given to this field type.
     * @param minimumOccurs   the minimum number of occurrences of the property within its containing entity.
     * @param maximumOccurs   the maximum number of occurrences of the property within its containing entity,
     *                        or {@link Integer#MAX_VALUE} if there is no restriction.
     */
    FieldType(final Map<String,?> identification, final int minimumOccurs, final int maximumOccurs) {
        super(identification);
        if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        this.minimumOccurs = minimumOccurs;
        this.maximumOccurs = maximumOccurs;
    }

    /**
     * Returns the minimum number of occurrences of the property within its containing entity.
     * The returned value is greater than or equal to zero.
     *
     * @return the minimum number of occurrences of the property within its containing entity.
     */
    public int getMinimumOccurs() {
        return minimumOccurs;
    }

    /**
     * Returns the maximum number of occurrences of the property within its containing entity.
     * The returned value is greater than or equal to the {@link #getMinimumOccurs()} value.
     * If there is no maximum, then this method returns {@link Integer#MAX_VALUE}.
     *
     * @return the maximum number of occurrences of the property within its containing entity,
     *         or {@link Integer#MAX_VALUE} if none.
     */
    public int getMaximumOccurs() {
        return maximumOccurs;
    }

    /**
     * Returns a hash code value for this property type.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37*(minimumOccurs + 31*maximumOccurs);
    }

    /**
     * Compares this property type with the given object for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            final FieldType that = (FieldType) obj;
            return minimumOccurs == that.minimumOccurs &&
                   maximumOccurs == that.maximumOccurs;
        }
        return false;
    }

    /**
     * Helper method for implementation of {@code PropertyType.toString()} methods.
     * Example:
     *
     * <pre class="text">PropertyType[“name” : ValueClass]</pre>
     *
     * @param deprecated  whether the type to format is deprecated.
     * @param className   the interface name of the object on which {@code toString()} is invoked.
     * @param name        the property type name, sometimes {@link #getName()} or sometimes the name of another object.
     * @param valueType   the name of value class (attribute), or the feature type name (association).
     */
    static StringBuilder toString(final boolean deprecated, final String className, final GenericName name, final Object valueType) {
        final StringBuilder buffer = new StringBuilder(40);
        if (deprecated) {
            buffer.append("Deprecated");
        }
        buffer.append(className).append('[');
        if (name != null) {
            buffer.append('“');
        }
        buffer.append(name);
        if (name != null) {
            buffer.append("” : ");
        }
        return buffer.append(valueType).append(']');
    }

    /**
     * Helper method for implementation of {@code Property.toString()} methods.
     * Example:
     *
     * <pre class="text">Property[“name” : ValueClass] = {value1, value2, ...}</pre>
     *
     * @param deprecated  whether the type to format is deprecated.
     * @param className   the interface name of the object on which {@code toString()} is invoked.
     * @param name        the property type name, sometimes {@link #getName()} or sometimes the name of another object.
     * @param valueType   the name of value class (attribute), or the feature type name (association).
     * @param values      the actual values.
     */
    static StringBuilder toString(final boolean deprecated, final String className, final GenericName name,
            final Object valueType, final Iterator<?> values)
    {
        final StringBuilder buffer = toString(deprecated, className, name, valueType);
        if (values.hasNext()) {
            final Object value = values.next();
            final boolean isMultiValued = values.hasNext();
            buffer.append(" = ");
            if (isMultiValued) {
                buffer.append('{');
            }
            buffer.append(value);
            if (isMultiValued) {
                buffer.append(", ").append(values.next());
                if (values.hasNext()) {
                    buffer.append(", ...");
                }
                buffer.append('}');
            }
        }
        return buffer;
    }
}
