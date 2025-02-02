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
package org.apache.sis.internal.book;


/**
 * Utilities related to the handling of characters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
public final class Characters {
    /**
     * Hyphen character to be visible only if there is a line break to insert after it
     * (Unicode {@code 00AD}, HTML {@code &shy;}). Otherwise this character is invisible.
     * When visible, the graphical symbol is similar to the hyphen character.
     *
     * <p>Note: {@link Character#isIdentifierIgnorable(int)} returns {@code true} for this character.</p>
     */
    public static final char SOFT_HYPHEN = '\u00AD';

    /**
     * Invisible space. Used for allowing line break in an identifier.
     *
     * <p>Note: {@link Character#isIdentifierIgnorable(int)} returns {@code true} for this character.</p>
     */
    public static final char ZERO_WIDTH_SPACE = '\u200B';

    /**
     * Do not allow instantiation of this class.
     */
    private Characters() {
    }
}
