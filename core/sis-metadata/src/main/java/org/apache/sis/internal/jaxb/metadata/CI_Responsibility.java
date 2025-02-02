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
package org.apache.sis.internal.jaxb.metadata;

import jakarta.xml.bind.annotation.XmlElementRef;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.FilterByVersion;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @since   1.0
 * @since   0.5
 */
public final class CI_Responsibility extends PropertyType<CI_Responsibility, DefaultResponsibility> {
    /**
     * Empty constructor for JAXB only.
     */
    public CI_Responsibility() {
    }

    /**
     * Returns the type which is bound by this adapter.
     *
     * @return {@code Responsibility.class}
     */
    @Override
    protected Class<DefaultResponsibility> getBoundType() {
        return DefaultResponsibility.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CI_Responsibility(final DefaultResponsibility value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <cit:CI_Responsibility>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected CI_Responsibility wrap(final DefaultResponsibility value) {
        return new CI_Responsibility(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <cit:CI_Responsibility>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    @SuppressWarnings("deprecation")
    public DefaultResponsibility getElement() {
        if (FilterByVersion.CURRENT_METADATA.accept()) {
            if (metadata instanceof ResponsibleParty) {
                // Need to build new DefaultResponsibility object here — simply casting doesn't work.
                return new DefaultResponsibility(metadata);
            }
            return metadata;
        } else if (FilterByVersion.LEGACY_METADATA.accept()) {
            if (metadata instanceof DefaultResponsibleParty) {
                return metadata;
            } else {
                return new DefaultResponsibleParty(metadata);
            }
        } else {
            return null;
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public void setElement(final DefaultResponsibility value) {
        metadata = value;
    }
}
