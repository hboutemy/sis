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
 * French extensions defined by the <cite>Association Française de Normalisation</cite> (AFNOR)
 * which are now incorporated in latest ISO standard. We provide those extensions in an internal
 * package because they should not be needed anymore except for backward compatibility with old
 * standard.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.4
 *
 * @see org.apache.sis.profile.france
 *
 * @since 0.4
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = FrenchProfile.NAMESPACE,
           xmlns = {@XmlNs(prefix = "fra", namespaceURI = FrenchProfile.NAMESPACE)})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class)
})
package org.apache.sis.internal.profile.fra;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.internal.jaxb.metadata.CI_Citation;
import org.apache.sis.profile.france.FrenchProfile;
