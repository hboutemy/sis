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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Apply syntax colorization on Java code. This class is different than most other colorization tools
 * since its apply different colors depending on whether a word is known to be defined in an OGC/ISO
 * standard, in GeoAPI or in Apache SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.7
 */
public final class CodeColorizer {
    /**
     * Lists of Java keywords.
     */
    public static final Set<String> JAVA_KEYWORDS = Set.of(
        "abstract", "continue", "for",        "new",        "switch",
        "assert",   "default",  "goto",       "package",    "synchronized",
        "boolean",  "do",       "if",         "private",    "this",
        "break",    "double",   "implements", "protected",  "throw",
        "byte",     "else",     "import",     "public",     "throws",
        "case",     "enum",     "instanceof", "return",     "transient",
        "catch",    "extends",  "int",        "short",      "try",
        "char",     "final",    "interface",  "static",     "void",
        "class",    "finally",  "long",       "strictfp",   "volatile",
        "const",    "float",    "native",     "super",      "while",
        /* literals: */ "true", "false", "null");

    /**
     * Returns all nodes in the given list as an array. This method is used for getting a snapshot
     * of the list before to modify it (for example before the elements are moved to another node).
     */
    static Node[] toArray(final NodeList nodes) {
        final Node[] children = new Node[nodes.getLength()];
        for (int i=0; i<children.length; i++) {
            children[i] = nodes.item(i);
        }
        return children;
    }

    /**
     * The specifier of an identifier.
     */
    private enum Specifier {
        OGC("OGC"), GEOAPI("GeoAPI"), SIS("SIS"), XML_PREFIX(null);

        /** The value to put in the {@code class} attribute of {@code <code>} or other elements, or {@code null} if none. */
        final String style;

        /** Creates a new enum to be rendered with the given style. */
        private Specifier(final String style) {
            this.style = style;
        }
    };

    /**
     * Map of predefined identifiers and the authority who defined them.
     */
    private final Map<String,Specifier> identifierSpecifiers;

    /**
     * The object to use for creating nodes.
     */
    private final Document document;

    /**
     * Creates a new color colorizer.
     *
     * @param  document  the object to use for creating nodes.
     * @throws IOException if an error occurred while reading the list of predefined identifiers.
     * @throws BookException if an identifier is defined twice.
     */
    public CodeColorizer(final Document document) throws IOException, BookException {
        this.document = document;
        identifierSpecifiers = new HashMap<>(1000);
        for (final Specifier specifier : Specifier.values()) {
            final String filename = specifier.name() + ".lst";
            final InputStream in = CodeColorizer.class.getResourceAsStream(filename);
            if (in == null) {
                throw new FileNotFoundException(filename);
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (identifierSpecifiers.put(line, specifier) != null) {
                        throw new BookException(line + " is defined twice in " + specifier);
                    }
                }
            }
        }
    }

    /**
     * Returns the re-defined identifiers and authority who defined it for the given word.
     *
     * @param  word  the word for which to get a specifier.
     * @return the specifier for the given word, or {@code null} if none.
     */
    private Specifier getSpecifier(String word) {
        StringBuilder buffer = null;
        final int length = word.length();
        for (int i=0; i<length; ) {
            final int c = word.codePointAt(i);
            if (Character.isIdentifierIgnorable(c)) {
                if (buffer == null) {
                    buffer = new StringBuilder(length).append(word, 0, i);
                }
            } else if (buffer != null) {
                buffer.appendCodePoint(c);
            }
            i += Character.charCount(c);
        }
        if (buffer != null) {
            word = buffer.toString();
        }
        return identifierSpecifiers.get(word);
    }

    /**
     * Returns {@code true} if the given string starts with the given prefix,
     * and the character following the prefix is not an identifier character.
     */
    private static boolean startsWithWord(final String string, final String prefix) {
        return string.startsWith(prefix) && (string.length() <= prefix.length() ||
                !Character.isJavaIdentifierPart(string.codePointAt(prefix.length())));
    }

    /**
     * Returns {@code true} if the given string from {@code i} inclusive to {@code upper} exclusive
     * is a Java identifier. Ignore zero-width space and soft hyphen.
     */
    private static boolean isJavaIdentifier(final String identifier, int i, final int upper) {
        if (upper <= i) {
            return false;
        }
        int c = identifier.codePointAt(i);
        if (!Character.isJavaIdentifierStart(c)) {
            return false;
        }
        while ((i += Character.charCount(c)) < upper) {
            c = Character.codePointAt(identifier, i);
            if (!Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the value to put inside in {@code class} attribute of a {@code <code>} element
     * encompassing the given identifier.
     *
     * <p>This method differs from {@link #highlight(Node, String)} in that it is used for applying
     * a single style on the whole string. By contrast, {@code highlight(…)} parses the text and may
     * apply different styles for different words.</p>
     */
    final String styleForSingleIdentifier(String word) {
        if (word.isEmpty()) {
            return null;
        }
        /*
         * If the given identifier is wrapped by some syntactic characters (e.g. "@Foo" for Java annotation,
         * or <Foo> for XML elements), remove the wrapper characters so we can get the identifier itelf.
         */
        switch (word.charAt(0)) {
            case '@': {
                word = word.substring(1);
                break;
            }
            case '<': {
                final int upper = word.length() - 1;
                if (word.charAt(upper) == '>') {
                    word = word.substring(1, upper);
                }
                break;
            }
            default: {
                int upper = word.length();
                while (upper > 0) {
                    final int c = word.codePointBefore(upper);
                    if (Character.isUnicodeIdentifierPart(c)) {
                        word = word.substring(0, upper);
                        break;
                    }
                    upper -= Character.charCount(c);
                }
            }
        }
        /*
         * Check if the keyword is a known one. The 'identifierOrigins' map contains only simple name
         * without package name or XML prefix. Fully qualified names are less commons but easier to
         * check since the package/prefix name is sufficient.
         */
        Specifier specifier = getSpecifier(word);
        if (specifier == null) {
            if (startsWithWord(word, "org.opengis") || startsWithWord(word, "geoapi")) {
                specifier = Specifier.GEOAPI;
            } else if (startsWithWord(word, "org.apache.sis") || startsWithWord(word, "sis")) {
                specifier = Specifier.SIS;
            } else {
                /*
                 * For more elaborated analysis than the above easy check, we need the Specifier enum of the
                 * first word. It may be a GeoAPI or SIS class name (e.g. "Citation" in "Citation.title"),
                 * or a XML prefix (e.g. "cit" in "cit:CI_Citation").
                 */
                int c, i=0;
                while (Character.isJavaIdentifierPart((c = word.charAt(i)))) {
                    i += Character.charCount(c);
                    if (i >= word.length()) {
                        return null;
                    }
                }
                specifier = getSpecifier(word.substring(0, i));
                switch (c) {
                    default: {
                        return null;
                    }
                    case '.': {
                        if (specifier != null && specifier.style != null) {
                            int s = word.lastIndexOf('(');
                            if (s < 0) s = word.length();
                            if (isJavaIdentifier(word, i+1, s)) {
                                break;
                            }
                        }
                        return null;
                    }
                    case ':': {
                        if (specifier == Specifier.XML_PREFIX) {
                            break;
                        }
                        return null;
                    }
                }
            }
        }
        /*
         * Found the specifier. The XML prefix case is handle in a special way because
         * we do not highlight the "cit", "gml", etc. prefixes in highlitht(…) method.
         */
        if (specifier == Specifier.XML_PREFIX) {
            specifier = Specifier.OGC;
        }
        return specifier.style;
    }

    /**
     * Applies emphasing on the words found in all text node of the given node.
     *
     * @param  parent  the root element where to put Java keywords in bold characters.
     *                 This is typically a {@code <samp>} or {@code <code>} element.
     * @param  type    {@code "xml"} if the element to process is XML rather than Java code.
     * @throws BookException if an element cannot be processed.
     */
    public void highlight(final Node parent, final String type) throws BookException {
        if ("wkt".equals(type)) return;
        final boolean isXML = "xml".equals(type);
        final boolean isJava = !isXML;                              // Future version may add more choices.
        Element syntacticElement = null;                            // E.g. comment block or a String.
        String  stopCondition    = null;                            // Identify 'syntacticElement' end.
        for (final Node node : toArray(parent.getChildNodes())) {
            /*
             * The following condition happen only if a quoted string or a comment started in a previous
             * node and is continuing in the current node. In such case we need to transfer everything we
             * found into the 'syntacticElement' node, until we found the 'stopCondition'.
             */
            if (stopCondition != null) {
                if (node.getNodeType() != Node.TEXT_NODE) {
                    syntacticElement.appendChild(node);         // Also remove from its previous position.
                    continue;
                }
                final String text = node.getTextContent();
                int lower = text.indexOf(stopCondition);
                if (lower >= 0) {
                    lower += stopCondition.length();
                    stopCondition = null;
                } else {
                    lower = text.length();
                }
                syntacticElement.appendChild(document.createTextNode(text.substring(0, lower)));
                node.setTextContent(text.substring(lower));
                // Continue below in case there is some remaining characters to analyse.
            }
            /*
             * The following is the usual code path where we search for Java keywords, OGC/ISO, GeoAPI and SIS
             * identifiers, and where the quoted strings and the comments are recognized.
             */
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE: {
                    highlight(node, type);
                    break;
                }
                case Node.TEXT_NODE: {
                    final String text = node.getTextContent();
                    int nextSubstringStart = 0, lower = 0;
                    while (lower < text.length()) {
                        int c = text.codePointAt(lower);
                        if (!Character.isJavaIdentifierStart(c)) {
                            if (c == '"') {
                                stopCondition = "\"";
                                syntacticElement = document.createElement("i");
                            } else if (isJava && text.startsWith("/*", lower)) {
                                stopCondition = "*/";
                                syntacticElement = document.createElement("code");
                                syntacticElement.setAttribute("class", "comment");
                            } else if (isJava && text.startsWith("//", lower)) {
                                stopCondition = "\n";
                                syntacticElement = document.createElement("code");
                                syntacticElement.setAttribute("class", "comment");
                            } else if (isXML && text.startsWith("<!--", lower)) {
                                stopCondition = "-->";
                                syntacticElement = document.createElement("code");
                                syntacticElement.setAttribute("class", "comment");
                            } else {
                                lower += Character.charCount(c);
                                continue;                       // "Ordinary" character: scan next characters.
                            }
                            /*
                             * Found the begining of a comment block or a string. Search where that block ends
                             * (it may be in another node) and store all text between the current position and
                             * the end into 'syntacticElement'.
                             */
                            if (nextSubstringStart != lower) {
                                parent.insertBefore(document.createTextNode(text.substring(nextSubstringStart, lower)), node);
                            }
                            nextSubstringStart = lower;
                            lower = text.indexOf(stopCondition, lower+1);
                            if (lower >= 0) {
                                if (!stopCondition.equals("\n")) {
                                    lower += stopCondition.length();
                                }
                                stopCondition = null;
                            } else {
                                lower = text.length();
                                // Keep stopCondition; we will need to search for it in next nodes.
                            }
                            syntacticElement.setTextContent(text.substring(nextSubstringStart, lower));
                            parent.insertBefore(syntacticElement, node);
                            nextSubstringStart = lower;
                        } else {
                            /*
                             * Found the beginning of a Java identifier. Search where it ends.
                             */
                            int upper = lower;
                            while (Character.isJavaIdentifierPart(c)) {
                                upper += Character.charCount(c);
                                if (upper >= text.length()) {
                                    break;
                                }
                                c = text.codePointAt(upper);
                            }
                            /*
                             * The following code is executed for each word which is a valid Java identifier.
                             * Different kind of emphasis may be applied: bold for Java keywords, some colors
                             * for OGC/ISO classes, other colors for SIS classes, etc.
                             */
                            final String word = text.substring(lower, upper);
                            Element emphasis = null;
                            if (JAVA_KEYWORDS.contains(word)) {
                                emphasis = document.createElement("b");
                            } else if (isJava) {
                                final Specifier origin = getSpecifier(word);
                                if (origin != null) {
                                    emphasis = document.createElement("code");
                                    emphasis.setAttribute("class", origin.style);
                                }
                            }
                            if (emphasis != null) {
                                emphasis.setTextContent(word);
                                if (nextSubstringStart != lower) {
                                    parent.insertBefore(document.createTextNode(text.substring(nextSubstringStart, lower)), node);
                                }
                                parent.insertBefore(emphasis, node);
                                nextSubstringStart = upper;
                            }
                            lower = upper;
                        }
                    }
                    node.setTextContent(text.substring(nextSubstringStart));
                }
            }
        }
    }
}
