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
 * Symbology encoding for styling map data independently of their source.
 * The classes in this package are derived from
 * OGC 05-077r4 — <a href="https://www.ogc.org/standard/se/">Symbology Encoding</a> Implementation Specification 1.1.0.
 * That document defines an XML encoding that can be used for styling feature and coverage data.
 * The root elements are
 * {@link org.apache.sis.style.se1.FeatureTypeStyle} and
 * {@link org.apache.sis.style.se1.CoverageStyle}.
 * Those classes include different kinds of {@link org.apache.sis.style.se1.Symbolizer}.
 *
 * @todo Add {@code CoverageStyle}. May require a common parent with {@code FeatureTypeStyle}.
 *
 * <h2>Future evolution</h2>
 * This package defines a XML encoding.
 * It is not an abstract model for sophisticated styling.
 * Apache SIS temporarily uses the classes of the XML encoding as a style API,
 * but a future version may replace this API by a more abstract one.
 * A good candidate may be ISO 19117:2012 — Portrayal.
 * As of 2023, various OGC working groups are also working on new style API.
 * The final form of such API has not yet been settled down.
 *
 * <h2>Synchronization</h2>
 * Classes in this package are not thread-safe.
 * Synchronization, if desired, must be done by the caller.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlSchema(location="https://schemas.opengis.net/se/1.1.0/FeatureStyle.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.SE,
           xmlns = {
                @XmlNs(prefix = "se", namespaceURI = Namespaces.SE)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(ExpressionAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class),
    @XmlJavaTypeAdapter(UnitAdapter.class)
})
package org.apache.sis.style.se1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.internal.jaxb.gco.InternationalStringConverter;
import org.apache.sis.internal.jaxb.gco.UnitAdapter;
import org.apache.sis.xml.Namespaces;
