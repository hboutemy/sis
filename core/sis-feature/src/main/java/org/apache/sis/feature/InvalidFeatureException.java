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
package org.apache.sis.feature;

import org.opengis.util.InternationalString;
import org.apache.sis.util.LocalizedException;


/**
 * Thrown when a feature fails at least one conformance test.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see Features#validate(AbstractFeature)
 *
 * @since 0.7
 */
final class InvalidFeatureException extends IllegalArgumentException implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7288810679876346027L;

    /**
     * A description of the illegal feature.
     */
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
    private final InternationalString message;

    /**
     * Creates a new exception with the given explanation message.
     *
     * @param message  a description of the illegal feature.
     */
    InvalidFeatureException(final InternationalString message) {
        super(message.toString());
        this.message = message;
    }

    /**
     * Returns the message in various locales.
     *
     * @return the exception message.
     */
    @Override
    public InternationalString getInternationalMessage() {
        return message;
    }
}
