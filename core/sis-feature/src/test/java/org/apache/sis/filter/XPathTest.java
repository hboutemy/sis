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
package org.apache.sis.filter;

import org.apache.sis.internal.filter.XPath;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link XPath}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.4
 */
public final class XPathTest extends TestCase {
    /**
     * Tests {@link XPath#split(String)}.
     */
    @Test
    public void testSplit() {
        assertNull(XPath.split("property"));
        assertArrayEquals(new String[] {"/property"},                    XPath.split("/property").toArray());
        assertArrayEquals(new String[] {"Feature", "property", "child"}, XPath.split("Feature/property/child").toArray());
        assertArrayEquals(new String[] {"/Feature", "property"},         XPath.split("/Feature/property").toArray());
    }
}
