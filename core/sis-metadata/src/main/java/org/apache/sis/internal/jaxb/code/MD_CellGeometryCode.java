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
package org.apache.sis.internal.jaxb.code;

import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.metadata.spatial.CellGeometry;
import org.apache.sis.internal.jaxb.cat.CodeListAdapter;
import org.apache.sis.internal.jaxb.cat.CodeListUID;
import org.apache.sis.xml.Namespaces;


/**
 * JAXB adapter for {@link CellGeometry}
 * in order to wrap the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 *
 * @author  Cédric Briançon (Geomatys)
 * @version 1.4
 * @since   0.3
 */
public final class MD_CellGeometryCode extends CodeListAdapter<MD_CellGeometryCode, CellGeometry> {
    /**
     * Empty constructor for JAXB only.
     */
    public MD_CellGeometryCode() {
    }

    /**
     * Creates a new adapter for the given value.
     */
    private MD_CellGeometryCode(final CodeListUID value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return the wrapper for the code list value.
     */
    @Override
    protected MD_CellGeometryCode wrap(final CodeListUID value) {
        return new MD_CellGeometryCode(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code list class.
     */
    @Override
    protected Class<CellGeometry> getCodeListClass() {
        return CellGeometry.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return the value to be marshalled.
     */
    @Override
    @XmlElement(name = "MD_CellGeometryCode", namespace = Namespaces.MSR)
    public CodeListUID getElement() {
        return identifier;
    }

    /**
     * Invoked by JAXB on unmarshalling.
     *
     * @param  value  the unmarshalled value.
     */
    public void setElement(final CodeListUID value) {
        identifier = value;
    }
}
