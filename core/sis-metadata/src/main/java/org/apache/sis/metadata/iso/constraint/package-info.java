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
 * Information about legal and security constraints placed on data.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.constraint OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <h2>Overview</h2>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO 19115 metadata}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.constraint.DefaultConstraints         Constraints}<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints    Legal constraints}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.constraint.DefaultSecurityConstraints Security constraints}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.constraint.DefaultReleasability       Releasability}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.constraint.Restriction    Restriction}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.constraint.Classification Classification}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *             {@linkplain org.apache.sis.metadata.iso.constraint.DefaultConstraints         Constraints}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.constraint.DefaultReleasability       Releasability}<br>
 *             {@linkplain org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints    Legal constraints}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.constraint.Restriction                       Restriction} «code list»<br>
 *             {@linkplain org.apache.sis.metadata.iso.constraint.DefaultSecurityConstraints Security constraints}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.constraint.Classification                    Classification} «code list»<br>
 * </td></tr></table>
 *
 * <h2>Null values, nil objects and collections</h2>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element cannot be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <em>live</em>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/mco/1.0/mco.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MCO,
           xmlns = {
                @XmlNs(prefix = "mco", namespaceURI = Namespaces.MCO),      // Metadata for Constraints
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC)       // Metadata Common Classes
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_Responsibility.class),
    @XmlJavaTypeAdapter(MD_BrowseGraphic.class),
    @XmlJavaTypeAdapter(MD_ClassificationCode.class),
    @XmlJavaTypeAdapter(MD_RestrictionCode.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.constraint;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
