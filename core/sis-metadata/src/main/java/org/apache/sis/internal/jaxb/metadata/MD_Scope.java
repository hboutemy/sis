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

import java.net.URISyntaxException;
import jakarta.xml.bind.annotation.XmlElementRef;
import org.opengis.metadata.quality.Scope;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter in order to map implementing class with the GeoAPI interface.
 * See package documentation for more information about JAXB and interface.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.3
 */
public class MD_Scope extends PropertyType<MD_Scope, Scope> {
    /**
     * Empty constructor for JAXB only.
     */
    public MD_Scope() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code Scope.class}
     */
    @Override
    protected final Class<Scope> getBoundType() {
        return Scope.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_Scope(final Scope value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <mcc:MD_Scope>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_Scope wrap(final Scope value) {
        return new MD_Scope(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <mcc:MD_Scope>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public final DefaultScope getElement() {
        return DefaultScope.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public final void setElement(final DefaultScope value) {
        metadata = value;
    }

    /**
     * On unmarshalling, creates an instance of the deprecated
     * {@link org.apache.sis.metadata.iso.quality.DefaultScope} subclass.
     */
    public static final class Legacy extends MD_Scope {
        /** Empty constructor used only by JAXB. */
        public Legacy() {
        }

        /** Converts an adapter read from an XML stream. */
        @Override public Scope unmarshal(final MD_Scope value) throws URISyntaxException {
            return DefaultScope.castOrCopy(super.unmarshal(value));
        }
    }

    /**
     * Wraps the value only if marshalling an element from the ISO 19115:2014 metadata model.
     * Otherwise (i.e. if marshalling according legacy ISO 19115:2003 model), omits the element.
     */
    public static final class Since2014 extends MD_Scope {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected MD_Scope wrap(final Scope value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
