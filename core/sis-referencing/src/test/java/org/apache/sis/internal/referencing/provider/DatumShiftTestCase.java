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
package org.apache.sis.internal.referencing.provider;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;


/**
 * Base class of tests that need to load a datum shift grid. This base class provides a
 * {@link #getResourceAsConvertibleURL(String)} method for fetching the data in a form
 * convertible to {@link URI}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
public abstract class DatumShiftTestCase extends TestCase {
    /**
     * For subclass constructors only.
     */
    DatumShiftTestCase() {
    }

    /**
     * Finds resource of the given name as a URL convertible to a {@link URI}.
     * If the URL is not convertible, then this method declares the test as ignored.
     *
     * @param  name  name of the resource to get.
     * @return the requested resources.
     */
    public static URL getResourceAsConvertibleURL(final String name) {
        final URL file = DatumShiftTestCase.class.getResource(name);
        if (file == null) {
            fail("Test file \"" + name + "\" not found.");
        } else {
            assumeFalse("Cannot read grid data in a JAR file.", "jar".equals(file.getProtocol()));
        }
        return file;
    }

    /**
     * Finds resource of the given name as an URI. If the resource cannot be obtained because
     * the grid file is inside a JAR file, declares the test as ignored instead of failed.
     *
     * @param  name  name of the resource to get.
     * @return the requested resources.
     */
    static URI getResource(final String name) throws URISyntaxException {
        final URL file = getResourceAsConvertibleURL(name);
        if (file == null) {
            assumeFalse("Cannot read grid data in a JAR file.", "jar".equals(file.getProtocol()));
        }
        return file.toURI();
    }
}
