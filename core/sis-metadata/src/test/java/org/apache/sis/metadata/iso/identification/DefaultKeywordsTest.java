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
package org.apache.sis.metadata.iso.identification;

import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultKeywords}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 */
public final class DefaultKeywordsTest extends TestCase {
    /**
     * Tests {@link DefaultKeywords#DefaultKeywords(CharSequence[])}.
     */
    @Test
    public void testConstructor() {
        final DefaultKeywords keywords = new DefaultKeywords("Keyword 1", "Keyword 2", "Keyword 3");
        assertArrayEquals(new Object[] {
            new SimpleInternationalString("Keyword 1"),
            new SimpleInternationalString("Keyword 2"),
            new SimpleInternationalString("Keyword 3")
        }, keywords.getKeywords().toArray());
    }
}
