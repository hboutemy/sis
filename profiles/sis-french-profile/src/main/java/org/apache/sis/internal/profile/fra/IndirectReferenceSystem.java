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
package org.apache.sis.internal.profile.fra;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.util.ComparisonMode;


/**
 * AFNOR extension to ISO {@link ReferenceSystem}.
 * The following schema fragment specifies the expected content contained within this class.
 *
 * {@snippet lang="xml" :
 *   <complexType name="FRA_IndirectReferenceSystem_Type">
 *     <complexContent>
 *       <extension base="{http://www.isotc211.org/2005/gmd}MD_ReferenceSystem_Type">
 *       </extension>
 *     </complexContent>
 *   </complexType>
 *   }
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.4
 * @since   0.4
 */
@XmlType(name = "FRA_IndirectReferenceSystem_Type")
@XmlRootElement(name= "FRA_IndirectReferenceSystem")
public class IndirectReferenceSystem extends ReferenceSystemMetadata {
    /**
     * For serialization purpose.
     */
    private static final long serialVersionUID = 177802130150613930L;

    /**
     * Empty constructor for JAXB.
     */
    public IndirectReferenceSystem() {
    }

    /**
     * Creates a new reference system from the given one.
     *
     * @param  crs  the reference system to partially copy.
     */
    public IndirectReferenceSystem(final ReferenceSystem crs) {
        super(crs);
    }

    /**
     * Creates a new reference system from the given code.
     *
     * @param  identifier  the reference system identifier.
     */
    public IndirectReferenceSystem(final ReferenceIdentifier identifier) {
        super(identifier);
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param  object  the object to compare with this reference system.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return super.equals(object, mode) && (object instanceof IndirectReferenceSystem);
    }
}
