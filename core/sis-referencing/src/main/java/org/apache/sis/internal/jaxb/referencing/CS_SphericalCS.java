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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.referencing.cs.SphericalCS;
import org.apache.sis.referencing.cs.DefaultSphericalCS;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class CS_SphericalCS extends PropertyType<CS_SphericalCS, SphericalCS> {
    /**
     * Empty constructor for JAXB only.
     */
    public CS_SphericalCS() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code SphericalCS.class}
     */
    @Override
    protected Class<SphericalCS> getBoundType() {
        return SphericalCS.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CS_SphericalCS(final SphericalCS cs) {
        super(cs);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:SphericalCS>} XML element.
     *
     * @param  cs  the element to marshall.
     * @return a {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CS_SphericalCS wrap(final SphericalCS cs) {
        return new CS_SphericalCS(cs);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:SphericalCS>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the element to be marshalled.
     */
    @XmlElement(name = "SphericalCS")
    public DefaultSphericalCS getElement() {
        return DefaultSphericalCS.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  cs  the unmarshalled element.
     */
    public void setElement(final DefaultSphericalCS cs) {
        metadata = cs;
    }
}
