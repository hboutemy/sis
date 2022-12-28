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
package org.apache.sis.referencing;


/**
 * Unchecked exception thrown when an error occurred while computing a geodetic value.
 * This exception may be used in contexts where a checked exception cannot be thrown.
 * This exception typically has a {@link org.opengis.util.FactoryException} or
 * {@link org.opengis.referencing.operation.TransformException} as its cause.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see GeodeticCalculator
 *
 * @since 1.0
 */
public class GeodeticException extends RuntimeException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7844524593329236175L;

    /**
     * Constructs a new exception with no message.
     */
    public GeodeticException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public GeodeticException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause  the cause, or {@code null} if none.
     */
    public GeodeticException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message  the detail message, or {@code null} if none.
     * @param cause    the cause, or {@code null} if none.
     */
    public GeodeticException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
