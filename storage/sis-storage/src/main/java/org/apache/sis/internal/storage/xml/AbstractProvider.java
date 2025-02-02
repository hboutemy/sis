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
package org.apache.sis.internal.storage.xml;

import java.util.Map;
import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.DocumentedStoreProvider;


/**
 * Base class for providers of {@link DataStore} implementations for XML files.
 * This base class does not assume that the data store will use any particular framework
 * (JAXB, StAX, <i>etc</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
public abstract class AbstractProvider extends DocumentedStoreProvider {
    /**
     * The {@value} MIME type, used only if {@link #probeContent(StorageConnector)} cannot determine
     * a more accurate type.
     */
    public static final String MIME_TYPE = "application/xml";

    /**
     * The read-ahead limit when reading the XML document from a {@link Reader}.
     */
    private static final int READ_AHEAD_LIMIT = 2048;

    /**
     * The expected XML header. According XML specification, this declaration is required to appear
     * at the document beginning (no space allowed before the declaration).
     */
    private static final byte[] HEADER = {'<','?','x','m','l',' '};

    /**
     * The mapping from XML namespaces to MIME types.
     *
     * <table class="sis">
     *   <caption>Example</caption>
     *   <tr><th>Key</th>                                  <th>Value</th></tr>
     *   <tr><td>http://www.opengis.net/gml/3.2</td>       <td>application/gml+xml</td></tr>
     *   <tr><td>http://www.isotc211.org/2005/gmd</td>     <td>application/vnd.iso.19139+xml</td></tr>
     *   <tr><td>http://www.opengis.net/cat/csw/2.0.2</td> <td>application/vnd.ogc.csw_xml</td></tr>
     * </table>
     */
    protected final Map<String,String> mimeForNameSpaces;

    /**
     * The mapping from root elements to MIME types. Used only if the root element is in
     * the default namespace and contains no {@code xmlns} attributes for that namespace.
     *
     * <table class="sis">
     *   <caption>Example</caption>
     *   <tr><th>Key</th>         <th>Value</th></tr>
     *   <tr><td>MD_Metadata</td> <td>application/vnd.iso.19139+xml</td></tr>
     * </table>
     */
    protected final Map<String,String> mimeForRootElements;

    /**
     * Creates a new provider.
     *
     * @param  name  the primary key to use for searching in the {@code MD_Format} table, or {@code null} if none.
     * @param  mimeForNameSpaces    the mapping from XML namespaces to MIME type.
     * @param  mimeForRootElements  the mapping from root elements to MIME types, used only as a fallback.
     */
    protected AbstractProvider(final String name, final Map<String,String> mimeForNameSpaces, final Map<String,String> mimeForRootElements) {
        super(name);
        this.mimeForNameSpaces   = mimeForNameSpaces;
        this.mimeForRootElements = mimeForRootElements;
    }

    /**
     * Returns the MIME type if the given storage appears to be supported by the data store.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable as a XML file.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        /*
         * Usual case. This includes InputStream, DataInput, File, Path, URL, URI.
         */
        Prober<ByteBuffer> prober = (buffer) -> {
            if (buffer.remaining() < HEADER.length) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            // Quick check for "<?xml " header.
            for (int i=0; i<HEADER.length; i++) {
                if (buffer.get() != HEADER[i]) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            // Now check for a more accurate MIME type.
            return new MimeTypeDetector(mimeForNameSpaces, mimeForRootElements) {
                @Override int read() {
                    if (buffer.hasRemaining()) {
                        return buffer.get();
                    }
                    insufficientBytes = (buffer.limit() != buffer.capacity());
                    return -1;
                }
            }.probeContent();
        };
        /*
         * We should enter in the "or else" block only if the user gave us explicitly a `Reader`.
         * A common case is a `StringReader` wrapping a `String` object.
         */
        return probeContent(connector, ByteBuffer.class, prober.orElse(Reader.class, (reader) -> {
            // Quick check for "<?xml " header.
            for (int i=0; i<HEADER.length; i++) {
                if (reader.read() != HEADER[i]) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            // Now check for a more accurate MIME type.
            return new MimeTypeDetector(mimeForNameSpaces, mimeForRootElements) {
                private int remaining = READ_AHEAD_LIMIT;
                @Override int read() throws IOException {
                    return (--remaining >= 0) ? IOUtilities.readCodePoint(reader) : -1;
                }
            }.probeContent();
        }));
    }
}
