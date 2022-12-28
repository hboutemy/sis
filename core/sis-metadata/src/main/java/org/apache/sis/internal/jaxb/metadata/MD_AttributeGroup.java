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

import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.metadata.content.AttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 */
public final class MD_AttributeGroup extends PropertyType<MD_AttributeGroup, AttributeGroup> {
    /**
     * Empty constructor for JAXB only.
     */
    public MD_AttributeGroup() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code AttributeGroup.class}
     */
    @Override
    protected Class<AttributeGroup> getBoundType() {
        return AttributeGroup.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_AttributeGroup(final AttributeGroup value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <mrc:MD_AttributeGroup>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_AttributeGroup wrap(final AttributeGroup value) {
        return new MD_AttributeGroup(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <mrc:MD_AttributeGroup>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultAttributeGroup getElement() {
        return DefaultAttributeGroup.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public void setElement(final DefaultAttributeGroup value) {
        metadata = value;
    }
}
