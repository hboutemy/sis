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
import org.opengis.metadata.citation.Series;
import org.apache.sis.metadata.iso.citation.DefaultSeries;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 */
public final class CI_Series extends PropertyType<CI_Series, Series> {
    /**
     * Empty constructor for JAXB only.
     */
    public CI_Series() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code Series.class}
     */
    @Override
    protected Class<Series> getBoundType() {
        return Series.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CI_Series(final Series value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <cit:CI_Series>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected CI_Series wrap(final Series value) {
        return new CI_Series(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <cit:CI_Series>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultSeries getElement() {
        return DefaultSeries.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public void setElement(final DefaultSeries value) {
        metadata = value;
    }
}
