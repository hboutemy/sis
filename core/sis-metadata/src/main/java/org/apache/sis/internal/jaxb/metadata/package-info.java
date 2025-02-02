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

/**
 * JAXB adapters for metadata. The class defined in this package are both JAXB adapters
 * replacing GeoAPI interfaces by SIS implementation classes at marshalling time (since
 * JAXB cannot marshal directly interfaces), and wrappers around the value to be marshalled.
 * ISO 19139 have the strange habit to wrap every properties in an extra level, for example:
 *
 * {@snippet lang="xml" :
 *   <CI_ResponsibleParty>
 *     <contactInfo>
 *       <CI_Contact>
 *         ...
 *       </CI_Contact>
 *     </contactInfo>
 *   </CI_ResponsibleParty>
 *   }
 *
 * The {@code </CI_Contact>} level is not really necessary, and JAXB is not designed for inserting
 * such level since it is not the usual way to write XML. In order to get this output with JAXB, we
 * have to wrap metadata object in an additional object. Those additional objects are defined in
 * this package.
 *
 * <p>So each class in this package is both a JAXB adapter and a wrapper. We have merged those
 * functionalities in order to avoid doubling the amount of classes, which is already large.</p>
 *
 * <p>In ISO 19139 terminology:</p>
 * <ul>
 *   <li>the public classes defined in the {@code org.apache.sis.metadata.iso} packages are defined
 *       as {@code Foo_Type} in ISO 19139, where <var>Foo</var> is the ISO name of a class.</li>
 *   <li>the internal classes defined in this package are defined as {@code Foo_PropertyType} in
 *       ISO 19139 schemas.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @author  Alexis Gaillard (Geomatys)
 * @since   1.3
 *
 * @see jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
 *
 * @since 0.3
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, xmlns = {
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
package org.apache.sis.internal.jaxb.metadata;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import org.apache.sis.xml.Namespaces;
