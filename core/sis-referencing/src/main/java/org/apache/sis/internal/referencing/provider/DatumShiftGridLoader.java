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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import org.opengis.util.FactoryException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;


/**
 * Base class of datum shift grid loaders.
 * This loader uses {@link ReadableByteChannel}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.7
 */
abstract class DatumShiftGridLoader {
    /**
     * Conversion factor from degrees to seconds.
     */
    static final double DEGREES_TO_SECONDS = 3600;

    /**
     * Possible precision for offset values in seconds of angle. This value is used only as a hint
     * when attempting to compress the grid in arrays of {@code short} values. It does not hurt if
     * this value is wrong, as it will only cause the grid to not be compressed.
     *
     * <p>Some interesting values:</p>
     * <ul>
     *   <li>1E-4 is about 3 millimetres on Earth.</li>
     *   <li>1E-6 matches the precision found in ASCII outputs of NADCON grids.</li>
     *   <li>1E-7 is about 1 ULP of 1 second of angle.</li>
     * </ul>
     *
     * We use a value of 1E-4 because more accurate values tend to cause overflows in the compression algorithm,
     * in which case the compression fails. With a more reasonable value, we have better chances of success.
     */
    static final double SECOND_PRECISION = 1E-4;

    /**
     * The file to load, used for parameter declaration and if we have errors to report.
     */
    final URI file;

    /**
     * The channel opened on the file.
     */
    private final ReadableByteChannel channel;

    /**
     * The buffer to use for transferring data from the channel.
     */
    final ByteBuffer buffer;

    /**
     * Whether the tip about the location of datum shift files has been logged.
     * We log this tip only once, and only if we failed to load at least one grid.
     */
    private static final AtomicBoolean datumDirectoryLogged = new AtomicBoolean();

    /**
     * Creates a new loader for the given channel and an existing buffer.
     *
     * @param  channel  where to read data from.
     * @param  buffer   the buffer to use.
     * @param  file     path to the longitude or latitude difference file. Used for parameter declaration and error reporting.
     */
    DatumShiftGridLoader(final ReadableByteChannel channel, final ByteBuffer buffer, final URI file) throws IOException {
        this.file    = file;
        this.buffer  = buffer;
        this.channel = channel;
        channel.read(buffer);
        buffer.flip();
    }

    /**
     * Makes sure that the buffer contains at least <var>n</var> remaining bytes.
     * It is caller's responsibility to ensure that the given number of bytes is
     * not greater than the {@linkplain ByteBuffer#capacity() buffer capacity}.
     *
     * @param  n  the minimal number of bytes needed in the {@linkplain #buffer}.
     * @throws EOFException if the channel has reached the end of stream.
     * @throws IOException if another kind of error occurred while reading.
     */
    final void ensureBufferContains(int n) throws IOException {
        assert n >= 0 && n <= buffer.capacity() : n;
        n -= buffer.remaining();
        if (n > 0) {
            buffer.compact();
            do {
                final int c = channel.read(buffer);
                if (c <= 0) {
                    if (c != 0) {
                        throw new EOFException(Errors.format(Errors.Keys.UnexpectedEndOfFile_1, file));
                    } else {
                        throw new IOException(Errors.format(Errors.Keys.CanNotRead_1, file));
                    }
                }
                n -= c;
            } while (n > 0);
            buffer.flip();
        }
    }

    /**
     * Skips exactly <var>n</var> bytes.
     */
    final void skip(int n) throws IOException {
        int p;
        while ((p = buffer.position() + n) > buffer.limit()) {
            n -= buffer.remaining();
            buffer.clear();
            ensureBufferContains(Math.min(n, buffer.capacity()));
        }
        buffer.position(p);
    }

    /**
     * If the given URI is not absolute, tries to make it absolute
     * with a path to the common directory of datum shift grid files.
     *
     * @param  path  the URI to make absolute.
     * @return an absolute (if possible) URI to the data.
     * @throws NoSuchFileException if the path cannot be made absolute.
     *         This exception is necessary for letting the caller know that the coordinate operation is
     *         probably valid but cannot be constructed because an optional configuration is missing.
     *         It is typically because the {@code SIS_DATA} environment variable has not been set.
     */
    static URI toAbsolutePath(final URI path) throws NoSuchFileException {
        if (path.isAbsolute()) {
            return path;
        }
        String message;
        if (path.isOpaque()) {
            message = Errors.format(Errors.Keys.CanNotOpen_1, path);
        } else {
            final Path dir = DataDirectory.DATUM_CHANGES.getDirectory();
            if (dir != null) {
                return dir.resolve(path.getPath()).toUri();
            }
            final String env = DataDirectory.getenv();
            if (env == null) {
                message = Messages.format(Messages.Keys.DataDirectoryNotSpecified_1, DataDirectory.ENV);
            } else {
                message = Messages.format(Messages.Keys.DataDirectoryNotReadable_2, DataDirectory.ENV, env);
            }
        }
        throw new NoSuchFileException(path.toString(), null, message);
    }

    /**
     * Creates a channel for reading bytes from the file at the specified path.
     *
     * @param  path  the path from where to read bytes.
     * @return a channel for reading bytes from the given path.
     * @throws IOException if the channel cannot be created.
     */
    static ReadableByteChannel newByteChannel(final URI path) throws IOException {
        try {
            return Files.newByteChannel(Path.of(path));
        } catch (FileSystemNotFoundException e) {
            Logging.ignorableException(AbstractProvider.LOGGER, DatumShiftGridLoader.class, "newByteChannel", e);
        }
        return Channels.newChannel(path.toURL().openStream());
    }

    /**
     * Logs a message about a grid which is about to be loaded.
     *
     * @param  caller  the provider to logs as the source class.
     *                 the source method will be set to {@code "createMathTransform"}.
     * @param  file    the grid file, as a {@link String} or a {@link URI}.
     */
    static void startLoading(final Class<?> caller, final Object file) {
        log(caller, Resources.forLocale(null).getLogRecord(Level.FINE, Resources.Keys.LoadingDatumShiftFile_1, file));
    }

    /**
     * Logs the given record.
     *
     * @param  caller  the provider to logs as the source class.
     *                 the source method will be set to {@code "createMathTransform"}.
     * @param  record  the record to complete and log.
     */
    static void log(final Class<?> caller, final LogRecord record) {
        Logging.completeAndLog(AbstractProvider.LOGGER, caller, "createMathTransform", record);
    }

    /**
     * Creates the exception to thrown when the provider failed to load the grid file.
     *
     * @param  format  the format name (e.g. "NTv2" or "NADCON").
     * @param  file    the grid file that the subclass tried to load.
     * @param  cause   the cause of the failure to load the grid file.
     */
    static FactoryException canNotLoad(final String format, final URI file, final Exception cause) {
        if (!datumDirectoryLogged.get()) {
            final Path directory = DataDirectory.DATUM_CHANGES.getDirectory();
            if (directory != null && !datumDirectoryLogged.getAndSet(true)) {
                final LogRecord record = Resources.forLocale(null).getLogRecord(
                        Level.INFO, Resources.Keys.DatumChangesDirectory_1, directory);

                // "readGrid" is actually defined by subclasses.
                Logging.completeAndLog(AbstractProvider.LOGGER, DatumShiftGridLoader.class, "readGrid", record);
            }
        }
        final boolean notFound = (cause instanceof NoSuchFileException);
        final String message = Resources.format(notFound ? Resources.Keys.FileNotFound_2
                                                         : Resources.Keys.FileNotReadable_2, format, file);
        if (notFound) {
            return new MissingFactoryResourceException(message, cause);
        } else {
            return new FactoryDataException(message, cause);
        }
    }
}
