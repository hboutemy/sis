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

import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.4
 */
public final class CD_PrimeMeridian extends PropertyType<CD_PrimeMeridian, PrimeMeridian> {
    /**
     * Empty constructor for JAXB only.
     */
    public CD_PrimeMeridian() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code PrimeMeridian.class}
     */
    @Override
    protected Class<PrimeMeridian> getBoundType() {
        return PrimeMeridian.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CD_PrimeMeridian(final PrimeMeridian datum) {
        super(datum);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:PrimeMeridian>} XML element.
     *
     * @param  datum  the element to marshal.
     * @return a {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CD_PrimeMeridian wrap(final PrimeMeridian datum) {
        return new CD_PrimeMeridian(datum);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:PrimeMeridian>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the element to be marshalled.
     */
    @XmlElement(name = "PrimeMeridian")
    public DefaultPrimeMeridian getElement() {
        return DefaultPrimeMeridian.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  datum  the unmarshalled element.
     */
    public void setElement(final DefaultPrimeMeridian datum) {
        metadata = datum;
        if (datum.getAngularUnit() == null) incomplete("angularUnit");
    }
}
