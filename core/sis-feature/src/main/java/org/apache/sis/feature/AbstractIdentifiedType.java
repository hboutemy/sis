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
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import java.io.Serializable;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.iso.Types;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Identification and description information inherited by property types and feature types.
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code IdentifiedType} interface in a future version.
 * When such interface will be available, most references to {@code AbstractIdentifiedType} in the API
 * will be replaced by references to the {@code IdentifiedType} interface.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.5
 */
public class AbstractIdentifiedType implements Deprecable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 277130188958446740L;

    /**
     * The logger used by feature implementations.
     */
    static final Logger LOGGER = Logger.getLogger(Modules.FEATURE);

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getName()}.
     *
     * @see #getName()
     */
    public static final String NAME_KEY = "name";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDefinition()}.
     *
     * @see #getDefinition()
     */
    public static final String DEFINITION_KEY = "definition";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDesignation()}.
     *
     * @see #getDesignation()
     */
    public static final String DESIGNATION_KEY = "designation";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDescription()}.
     *
     * @see #getDescription()
     */
    public static final String DESCRIPTION_KEY = "description";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #isDeprecated()}.
     *
     * <p>If this property is set to {@code true}, then the value associated to {@link #DESCRIPTION_KEY}
     * should give the replacement (e.g. <cite>"superceded by …"</cite>).</p>
     *
     * @see #isDeprecated()
     *
     * @since 0.8
     */
    public static final String DEPRECATED_KEY = "deprecated";

    /**
     * The name of this type.
     *
     * @see #getName()
     * @see #NAME_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final GenericName name;

    /**
     * Concise definition of the element.
     *
     * @see #getDefinition()
     * @see #DEFINITION_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final InternationalString definition;

    /**
     * Natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #name} in user interfaces.
     *
     * @see #getDesignation()
     * @see #DESIGNATION_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final InternationalString designation;

    /**
     * Optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * @see #getDescription()
     * @see #DESCRIPTION_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final InternationalString description;

    /**
     * {@code true} if this type is deprecated.
     *
     * @see #isDeprecated()
     * @see #DEPRECATED_KEY
     */
    final boolean deprecated;

    /**
     * Constructs a type from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the {@value #NAME_KEY}.
     * Other entries listed in the table below are optional.
     *
     * <table class="sis">
     *   <caption>Recognized map entries</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value #NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #DESIGNATION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #DESCRIPTION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   <tr>
     *     <td>{@value #DEPRECATED_KEY}</td>
     *     <td>{@link Boolean}</td>
     *     <td>{@link #isDeprecated()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
     *     <td>{@link Locale}</td>
     *     <td>(none)</td>
     *   </tr>
     * </table>
     *
     * <h4>Localization</h4>
     * All localizable attributes like {@code "definition"} may have a language and country code suffix.
     * For example, the {@code "definition_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
     * the {@code "definition_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     * They are convenience properties for building the {@code InternationalString} value.
     *
     * <p>The {@code "locale"} property applies only in case of exception for formatting the error message, and
     * is used only on a <cite>best effort</cite> basis. The locale is discarded after successful construction
     * since localizations are applied by the {@link InternationalString#toString(Locale)} method.</p>
     *
     * @param  identification  the name and other information to be given to this identified type.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    protected AbstractIdentifiedType(final Map<String,?> identification) throws IllegalArgumentException {
        ensureNonNull("identification", identification);
        Object value = identification.get(NAME_KEY);
        if (value == null) {
            throw new IllegalArgumentException(Errors.getResources(identification)
                    .getString(Errors.Keys.MissingValueForProperty_1, NAME_KEY));
        } else if (value instanceof String) {
            name = createName(DefaultNameFactory.provider(), (String) value);
        } else if (value instanceof GenericName) {
            name = (GenericName) value;
        } else {
            throw illegalPropertyType(identification, NAME_KEY, value);
        }
        definition  = Types.toInternationalString(identification, DEFINITION_KEY);
        designation = Types.toInternationalString(identification, DESIGNATION_KEY);
        description = Types.toInternationalString(identification, DESCRIPTION_KEY);
        value = identification.get(DEPRECATED_KEY);
        if (value == null) {
            deprecated = false;
        } else if (value instanceof Boolean) {
            deprecated = (Boolean) value;
        } else {
            throw illegalPropertyType(identification, DEPRECATED_KEY, value);
        }
    }

    /**
     * Returns the exception to be thrown when a property is of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(final Map<String,?> identification,
            final String key, final Object value)
    {
        return new IllegalArgumentException(Errors.getResources(identification).getString(
                Errors.Keys.IllegalPropertyValueClass_2, key, value.getClass()));
    }

    /**
     * Creates a name from the given string. This method is invoked at construction time,
     * so it should not use any field in this {@code AbtractIdentifiedObject} instance.
     */
    GenericName createName(final NameFactory factory, final String value) {
        return factory.createLocalName(null, value);
    }

    /**
     * Returns the name of this type.
     * The namespace can be either explicit
     * ({@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name}) or implicit
     * ({@linkplain org.apache.sis.util.iso.DefaultLocalName local name}).
     *
     * <p>For {@linkplain DefaultFeatureType feature types}, the name is mandatory and shall be unique
     * in the unit processing the data (e.g. a {@link org.apache.sis.storage.DataStore} reading a file).</p>
     *
     * <h4>API design note</h4>
     * This method is final because it is invoked (indirectly) by subclass constructors,
     * and invoking a user-overrideable method at construction time is not recommended.
     * Furthermore, this attribute is often used as the primary key for {@code IdentifiedType} instances
     * and need some guarantees about its stability.
     *
     * @return the type name.
     */
    public final GenericName getName() {
        return name;
    }

    /**
     * Returns a concise definition of the element.
     *
     * @return concise definition of the element.
     */
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Returns a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @return natural language designator for the element, or {@code null} if none.
     */
    public InternationalString getDesignation() {
        return designation;
    }

    /**
     * Returns optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * <p>If this type {@linkplain #isDeprecated() is deprecated}, then the description should give
     * indication about the replacement (e.g. <cite>"superceded by …"</cite>).</p>
     *
     * @return information beyond that required for concise definition of the element, or {@code null} if none.
     */
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Returns comments on or information about this type.
     * The default implementation performs the following choice:
     *
     * <ul>
     *   <li>If this type {@linkplain #isDeprecated() is deprecated}, returns the
     *       {@linkplain #getDescription() description}. The description of deprecated types
     *       should give indication about the replacement (e.g. <cite>"superceded by …"</cite>).</li>
     *   <li>Otherwise returns {@code null} since remarks are not part of the ISO 19109 feature model.</li>
     * </ul>
     *
     * @return the remarks, or {@code null} if none.
     *
     * @since 0.8
     */
    @Override
    public InternationalString getRemarks() {
        return deprecated ? description : null;
    }

    /**
     * Returns {@code true} if this type is deprecated.
     * If this method returns {@code true}, then the {@linkplain #getRemarks() remarks} should give
     * indication about the replacement (e.g. <cite>"superceded by …"</cite>).
     *
     * @return whether this type is deprecated.
     *
     * @since 0.8
     */
    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    /*
     * ISO 19109 properties omitted for now:
     *
     *   - constrainedBy : CharacterString
     *
     * Rational: a CharacterString is hardly programmatically usable. A Range would be better but too specific.
     * We could follow the GeoAPI path and define a "restrictions : Filter" property. That would be more generic,
     * but we are probably better to wait for Filter to be implemented in SIS.
     *
     * Reference: https://issues.apache.org/jira/browse/SIS-175
     */

    /**
     * Returns a hash code value for this type.
     *
     * @return the hash code for this type.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, definition, designation, description, deprecated);
    }

    /**
     * Compares this type with the given object for equality.
     *
     * @param  obj  the object to compare with this type.
     * @return {@code true} if the given object is equal to this type.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && getClass() == obj.getClass()) {
            final AbstractIdentifiedType that = (AbstractIdentifiedType) obj;
            return Objects.equals(name,        that.name) &&
                   Objects.equals(definition,  that.definition) &&
                   Objects.equals(designation, that.designation) &&
                   Objects.equals(description, that.description) &&
                   deprecated == that.deprecated;
        }
        return false;
    }

    /**
     * Returns the string representation of the given name, making sure that the name is non-null
     * and the string non-empty. This method is used for checking argument validity.
     *
     * @param  name       the name for which to get the string representation.
     * @param  container  the feature or attribute which contains the named characteristics.
     * @param  argument   the name of the argument ({@code "properties"} or {@code "characterizedBy"}).
     * @param  index      index of the characteristics having the given name.
     * @throws IllegalArgumentException if the given name is null or have an empty string representation.
     */
    static String toString(final GenericName name, final AbstractIdentifiedType container,
            final String argument, final int index)
    {
        short key = Errors.Keys.MissingValueForProperty_1;
        if (name != null) {
            final String s = name.toString();
            if (!s.isEmpty()) {
                return s;
            }
            key = Errors.Keys.EmptyProperty_1;
        }
        final StringBuilder b = new StringBuilder(40).append("Type[“").append(container.getName()).append("”].")
                .append(argument).append('[').append(index).append("].name");
        throw new IllegalArgumentException(Errors.format(key, b.toString()));
    }
}
