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
package org.apache.sis.metadata.iso.quality;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.TypeName;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.Namespaces;

// Branch-dependent imports
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Data quality basic measure.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQM_BasicMeasure}
 * {@code   ├─name……………………} Name of the data quality basic measure applied to the data.
 * {@code   ├─definition……} Definition of the data quality basic measure.
 * {@code   ├─example……………} Illustration of the use of a data quality measure.
 * {@code   └─valueType………} Value type for the result of the basic measure (shall be one of the data types defined in ISO/TS 19103:2005).</div>
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
 * @author  Alexis Gaillard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.3
 */
@XmlType(name = "DQM_BasicMeasure_Type", namespace = Namespaces.DQM, propOrder = {
    "name",
    "definition",
    "example",
    "valueType"
})
@XmlRootElement(name = "DQM_BasicMeasure", namespace = Namespaces.DQM)
@UML(identifier="DQM_BasicMeasure", specification=UNSPECIFIED)
public class DefaultBasicMeasure extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1665043206717367320L;

    /**
     * Name of the data quality basic measure applied to the data.
     */
    @SuppressWarnings("serial")
    private InternationalString name;

    /**
     * Definition of the data quality basic measure.
     */
    @SuppressWarnings("serial")
    private InternationalString definition;

    /**
     * Illustration of the use of a data quality measure.
     */
    @SuppressWarnings("serial")
    private DefaultMeasureDescription example;

    /**
     * Value type for the result of the basic measure.
     */
    @SuppressWarnings("serial")
    private TypeName valueType;

    /**
     * Constructs an initially empty basic measure.
     */
    public DefaultBasicMeasure() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultBasicMeasure(final DefaultBasicMeasure object) {
        super(object);
        if (object != null) {
            name       = object.getName();
            definition = object.getDefinition();
            example    = object.getExample();
            valueType  = object.getValueType();
        }
    }

    /**
     * Returns the name of the data quality basic measure.
     *
     * @return name of the data quality basic measure.
     */
    @XmlElement(name = "name", required = true)
    @UML(identifier="name", obligation=MANDATORY, specification=UNSPECIFIED)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the data quality basic measure.
     *
     * @param  newValue  the new basic measure name.
     */
    public void setName(final InternationalString newValue)  {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Returns the definition of the data quality basic measure.
     *
     * @return definition of the data quality basic measure.
     */
    @XmlElement(name = "definition", required = true)
    @UML(identifier="definition", obligation=MANDATORY, specification=UNSPECIFIED)
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Sets the definition of the data quality basic measure.
     *
     * @param  newValue  the new basic measure definition.
     */
    public void setDefinition(final InternationalString newValue)  {
        checkWritePermission(definition);
        definition = newValue;
    }

    /**
     * Returns the illustration of the use of a data quality measure.
     *
     * @return usage example, or {@code null} if none.
     */
    @XmlElement(name = "example")
    @UML(identifier="example", obligation=OPTIONAL, specification=UNSPECIFIED)
    public DefaultMeasureDescription getExample() {
        return example;
    }

    /**
     * Sets the illustration of the use of a data quality measure.
     *
     * @param  newValues  the new basic measure example.
     */
    public void setExample(final DefaultMeasureDescription newValues) {
        checkWritePermission(example);
        example = newValues;
    }

    /**
     * Returns the value type for the result of the basic measure.
     *
     * @return value type of the result for the basic measure.
     */
    @XmlElement(name = "valueType", required = true)
    @UML(identifier="valueType", obligation=MANDATORY, specification=UNSPECIFIED)
    public TypeName getValueType() {
        return valueType;
    }

    /**
     * Sets the value type for the result of the basic measure.
     *
     * @param  newValue  the new basic measure value type.
     */
    public void setValueType(final TypeName newValue)  {
        checkWritePermission(valueType);
        valueType = newValue;
    }
}
