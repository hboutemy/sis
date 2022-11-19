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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.xml.Namespaces;

// Branch-dependent imports
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Reference to the source of the data quality measure.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQM_SourceReference}
 * {@code   └─citation……………} References to the source.</div>
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
 * @version 1.3
 * @since   1.3
 * @module
 */
@XmlType(name = "DQM_SourceReference_Type", namespace = Namespaces.DQM)
@XmlRootElement(name = "DQM_SourceReference", namespace = Namespaces.DQM)
@UML(identifier="DQM_SourceReference", specification=UNSPECIFIED)
public class DefaultSourceReference extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2923526577209702000L;

    /**
     * References to the source.
     */
    @SuppressWarnings("serial")
    private Citation citation;

    /**
     * Constructs an initially empty source reference.
     */
    public DefaultSourceReference() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultSourceReference(final DefaultSourceReference object) {
        super(object);
        if (object != null) {
            citation = object.getCitation();
        }
    }

    /**
     * Returns the references to the source.
     *
     * @return reference to the source.
     */
    @XmlElement(name = "citation", required = true)
    @UML(identifier="citation", obligation=MANDATORY, specification=UNSPECIFIED)
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the references to the source.
     *
     * @param  newValue  the new source references.
     */
    public void setCitation(final Citation newValue)  {
        checkWritePermission(citation);
        citation = newValue;
    }
}
