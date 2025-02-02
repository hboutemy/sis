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
package org.apache.sis.metadata.iso.content;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.Record;
import org.opengis.util.InternationalString;
import org.opengis.metadata.content.RangeElementDescription;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.TitleProperty;


/**
 * Description of specific range elements.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MI_RangeElementDescription}
 * {@code   ├─name………………………………………………} Designation associated with a set of range elements.
 * {@code   ├─definition………………………………} Description of a set of specific range elements.
 * {@code   └─rangeElement…………………………} Specific range elements, i.e. range elements associated with a name and their definition.</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@TitleProperty(name = "name")
@XmlType(name = "MI_RangeElementDescription_Type", propOrder = {
    "name",
    "definition",
    "rangeElements"
})
@XmlRootElement(name = "MI_RangeElementDescription")
public class DefaultRangeElementDescription extends ISOMetadata implements RangeElementDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8891149098619355114L;

    /**
     * Designation associated with a set of range elements.
     */
    @SuppressWarnings("serial")
    private InternationalString name;

    /**
     * Description of a set of specific range elements.
     */
    @SuppressWarnings("serial")
    private InternationalString definition;

    /**
     * Specific range elements, i.e. range elements associated with a name and their definition.
     */
    @SuppressWarnings("serial")
    private Collection<Record> rangeElements;

    /**
     * Constructs an initially empty range element description.
     */
    public DefaultRangeElementDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(RangeElementDescription)
     */
    public DefaultRangeElementDescription(final RangeElementDescription object) {
        super(object);
        if (object != null) {
            name          = object.getName();
            definition    = object.getDefinition();
            rangeElements = copyCollection(object.getRangeElements(), Record.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRangeElementDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRangeElementDescription} instance is created using the
     *       {@linkplain #DefaultRangeElementDescription(RangeElementDescription) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRangeElementDescription castOrCopy(final RangeElementDescription object) {
        if (object == null || object instanceof DefaultRangeElementDescription) {
            return (DefaultRangeElementDescription) object;
        }
        return new DefaultRangeElementDescription(object);
    }

    /**
     * Returns the designation associated with a set of range elements.
     *
     * @return designation associated with a set of range elements, or {@code null}.
     */
    @Override
    @XmlElement(name = "name", required = true)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the designation associated with a set of range elements.
     *
     * @param  newValue  the new name value.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Returns the description of a set of specific range elements.
     *
     * @return description of a set of specific range elements, or {@code null}.
     */
    @Override
    @XmlElement(name = "definition", required = true)
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Sets the description of a set of specific range elements.
     *
     * @param  newValue  the new definition value.
     */
    public void setDefinition(final InternationalString newValue) {
        checkWritePermission(definition);
        definition = newValue;
    }

    /**
     * Returns the specific range elements, i.e. range elements associated with a name
     * and their definition.
     *
     * @return specific range elements.
     *
     * @todo implements {@link Record} in order to use the annotation.
     */
    @Override
    @XmlElement(name = "rangeElement", required = true)
    public Collection<Record> getRangeElements() {
        return rangeElements = nonNullCollection(rangeElements, Record.class);
    }

    /**
     * Sets the specific range elements, i.e. range elements associated with a name and their definition.
     *
     * @param  newValues  the new range element values.
     */
    public void setRangeElements(final Collection<? extends Record> newValues) {
        rangeElements = writeCollection(newValues, rangeElements, Record.class);
    }
}
