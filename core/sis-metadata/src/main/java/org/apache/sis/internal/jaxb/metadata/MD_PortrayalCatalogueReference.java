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
import org.opengis.metadata.PortrayalCatalogueReference;
import org.apache.sis.metadata.iso.DefaultPortrayalCatalogueReference;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.3
 */
public final class MD_PortrayalCatalogueReference extends
        PropertyType<MD_PortrayalCatalogueReference, PortrayalCatalogueReference>
{
    /**
     * Empty constructor for JAXB only.
     */
    public MD_PortrayalCatalogueReference() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code PortrayalCatalogueReference.class}
     */
    @Override
    protected Class<PortrayalCatalogueReference> getBoundType() {
        return PortrayalCatalogueReference.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_PortrayalCatalogueReference(final PortrayalCatalogueReference value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <mpc:MD_PortrayalCatalogueReference>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_PortrayalCatalogueReference wrap(final PortrayalCatalogueReference value) {
        return new MD_PortrayalCatalogueReference(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <mpc:MD_PortrayalCatalogueReference>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultPortrayalCatalogueReference getElement() {
        return DefaultPortrayalCatalogueReference.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public void setElement(final DefaultPortrayalCatalogueReference value) {
        metadata = value;
    }
}
