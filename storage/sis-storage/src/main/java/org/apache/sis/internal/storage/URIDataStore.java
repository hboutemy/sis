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
package org.apache.sis.internal.storage;

import java.net.URI;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystemNotFoundException;
import java.nio.charset.Charset;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;


/**
 * A data store for a storage that may be represented by a {@link URI}.
 * It is still possible to create a data store with an {@link java.nio.channels.ReadableByteChannel},
 * {@link java.io.InputStream} or {@link java.io.Reader}, in which case the {@linkplain #location} will be null.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
public abstract class URIDataStore extends DataStore implements StoreResource, ResourceOnFileSystem {
    /**
     * The {@link DataStoreProvider#LOCATION} parameter value, or {@code null} if none.
     */
    private final URI location;

    /**
     * Creates a new data store.
     *
     * @param  provider   the factory that created this {@code URIDataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected URIDataStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        location = connector.getStorageAs(URI.class);
    }

    /**
     * Returns the originator of this resource, which is this data store itself.
     *
     * @return {@code this}.
     */
    @Override
    public final DataStore getOriginator() {
        return this;
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component or an empty array if none.
     * The default implementation converts the URI to a {@link Path}. Note that if the original
     * {@link StorageConnector} argument given by user at construction time already contained a
     * {@link Path}, then the {@code Path} → {@code URI} → {@code Path} roundtrip is equivalent
     * to returning the {@link Path#toAbsolutePath()} value of user argument.
     *
     * @return the URI as a path, or an empty array if the URI is null.
     * @throws DataStoreException if the URI can not be converted to a {@link Path}.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        final Path path;
        if (location == null) {
            return new Path[0];
        } else try {
            path = Paths.get(location);
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            throw new DataStoreException(e);
        }
        return new Path[] {path};
    }

    /**
     * Returns the parameters used to open this data store.
     *
     * @return parameters used for opening this {@code DataStore}.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(parameters(provider, location));
    }

    /**
     * Creates parameter value group for the current location, if non-null.
     * This convenience method is used for {@link DataStore#getOpenParameters()} implementations in public
     * {@code DataStore} that can not extend {@code URIDataStore} directly, because this class is internal.
     *
     * @param  provider  the provider of the data store for which to get open parameters.
     * @param  location  file opened by the data store.
     * @return parameters to be returned by {@link DataStore#getOpenParameters()}.
     *
     * @todo Verify if non-exported classes in JDK9 are hidden from Javadoc, like package-private classes.
     *       If true, we could remove this hack and extend {@code URIDataStore} even in public classes.
     */
    public static ParameterValueGroup parameters(final DataStoreProvider provider, final URI location) {
        if (location == null || provider == null) return null;
        final ParameterValueGroup pg = provider.getOpenParameters().createValue();
        pg.parameter(DataStoreProvider.LOCATION).setValue(location);
        return pg;
    }

    /**
     * Provider for {@link URIDataStore} instances.
     *
     * @author  Johann Sorel (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.0
     * @since   0.8
     * @module
     */
    public abstract static class Provider extends DataStoreProvider {
        /**
         * Description of the {@value #LOCATION} parameter.
         */
        public static final ParameterDescriptor<URI> LOCATION_PARAM;

        /**
         * Description of the optional {@value #CREATE} parameter, which may be present in writable data store.
         * This parameter is not included in the descriptor created by {@link #build(ParameterBuilder)} default
         * implementation. It is subclass responsibility to add it if desired, only if supported.
         */
        public static final ParameterDescriptor<Boolean> CREATE_PARAM;

        /**
         * Description of the optional parameter for character encoding used by the data store.
         * This parameter is not included in the descriptor created by {@link #build(ParameterBuilder)}
         * default implementation. It is subclass responsibility to add it if desired.
         */
        public static final ParameterDescriptor<Charset> ENCODING;
        static {
            final ParameterBuilder builder = new ParameterBuilder();
            ENCODING       = builder.addName("encoding").setDescription(Resources.formatInternational(Resources.Keys.DataStoreEncoding)).create(Charset.class, null);
            CREATE_PARAM   = builder.addName( CREATE   ).setDescription(Resources.formatInternational(Resources.Keys.DataStoreCreate  )).create(Boolean.class, null);
            LOCATION_PARAM = builder.addName( LOCATION ).setDescription(Resources.formatInternational(Resources.Keys.DataStoreLocation)).setRequired(true).create(URI.class, null);
        }

        /**
         * The parameter descriptor to be returned by {@link #getOpenParameters()}.
         * Created when first needed.
         */
        private volatile ParameterDescriptorGroup openDescriptor;

        /**
         * Creates a new provider.
         */
        protected Provider() {
        }

        /**
         * Returns a description of all parameters accepted by this provider for opening a data store.
         * This method creates the descriptor only when first needed. Subclasses can override the
         * {@link #build(ParameterBuilder)} method if they need to modify the descriptor to create.
         *
         * @return description of the parameters required or accepted for opening a {@link DataStore}.
         */
        @Override
        public final ParameterDescriptorGroup getOpenParameters() {
            ParameterDescriptorGroup desc = openDescriptor;
            if (desc == null) {
                openDescriptor = desc = build(new ParameterBuilder().addName(getShortName()));
            }
            return desc;
        }

        /**
         * Invoked by {@link #getOpenParameters()} the first time that a parameter descriptor needs
         * to be created.  When invoked, the parameter group name is set to a name derived from the
         * {@link #getShortName()} value. The default implementation creates a group containing only
         * {@link #LOCATION_PARAM}. Subclasses can override if they need to create a group with more
         * parameters.
         *
         * @param  builder  the builder to use for creating parameter descriptor. The group name is already set.
         * @return the parameters descriptor created from the given builder.
         */
        protected ParameterDescriptorGroup build(final ParameterBuilder builder) {
            return builder.createGroup(LOCATION_PARAM);
        }

        /**
         * Convenience method creating a parameter descriptor containing only {@link #LOCATION_PARAM}.
         * This convenience method is used for public providers that can not extend this {@code Provider}
         * class because it is internal.
         *
         * @param  name  short name of the data store format.
         * @return the descriptor for open parameters.
         *
         * @todo Verify if non-exported classes in JDK9 are hidden from Javadoc, like package-private classes.
         *       If true, we could remove this hack and extend {@code URIDataStore} even in public classes.
         */
        public static ParameterDescriptorGroup descriptor(final String name) {
            return new ParameterBuilder().addName(name).createGroup(LOCATION_PARAM);
        }

        /**
         * Creates a storage connector initialized to the location declared in given parameters.
         * This convenience method does not set any other parameters.
         * In particular, reading (or ignoring) the {@value #CREATE} parameter is left to callers,
         * because not all implementations may create data stores with {@link java.nio.file.StandardOpenOption}.
         *
         * @param  provider    the provider for which to create a storage connector (for error messages).
         * @param  parameters  the parameters to use for creating a storage connector.
         * @return the storage connector initialized to the location specified in the parameters.
         * @throws IllegalOpenParameterException if no {@value #LOCATION} parameter has been found.
         */
        public static StorageConnector connector(final DataStoreProvider provider, final ParameterValueGroup parameters)
                throws IllegalOpenParameterException
        {
            ParameterNotFoundException cause = null;
            if (parameters != null) try {
                final Object location = parameters.parameter(LOCATION).getValue();
                if (location != null) {
                    return new StorageConnector(location);
                }
            } catch (ParameterNotFoundException e) {
                cause = e;
            }
            throw new IllegalOpenParameterException(Resources.format(Resources.Keys.UndefinedParameter_2,
                        provider.getShortName(), LOCATION), cause);
        }
    }

    /**
     * Returns the location (path, URL, URI, <i>etc.</i>) of the given resource.
     * The type of the returned object can be any of the types documented in {@link DataStoreProvider#LOCATION}.
     * The main ones are {@link java.net.URI}, {@link java.nio.file.Path} and JDBC {@linkplain javax.sql.DataSource}.
     *
     * @param  resource  the resource for which to get the location, or {@code null}.
     * @return location of the given resource, or {@code null} if none.
     * @throws DataStoreException if an error on the file system prevent the creation of the path.
     *
     * @since 1.1
     */
    public static Object location(final Resource resource) throws DataStoreException {
        if (resource instanceof DataStore) {
            final Optional<ParameterValueGroup> p = ((DataStore) resource).getOpenParameters();
            if (p.isPresent()) try {
                return p.get().parameter(DataStoreProvider.LOCATION).getValue();
            } catch (ParameterNotFoundException e) {
                /*
                 * This exception should not happen often since the "location" parameter is recommended.
                 * Note that it does not mean the same thing than "parameter provided but value is null".
                 * In that later case we want to return the null value as specified in the parameters.
                 */
                Logging.recoverableException(Logging.getLogger(Modules.STORAGE), URIDataStore.class, "location", e);
            }
        }
        /*
         * This fallback should not happen with `URIDataStore` implementation because the "location" parameter
         * is always present even if null. This fallback is for resources implementated by different classes.
         */
        if (resource instanceof ResourceOnFileSystem) {
            final Path[] paths = ((ResourceOnFileSystem) resource).getComponentFiles();
            if (paths != null && paths.length != 0) {
                return paths[0];                                    // First path is presumed the main file.
            }
        }
        return null;
    }

    /**
     * Adds the filename (without extension) as the citation title if there is no title, or as the identifier otherwise.
     * This method should be invoked last, after {@code DataStore} implementation did its best effort for adding a title.
     * The intent is actually to provide an identifier, but since the title is mandatory in ISO 19115 metadata, providing
     * only an identifier without title would be invalid.
     *
     * @param  builder  where to add the title or identifier.
     */
    protected final void addTitleOrIdentifier(final MetadataBuilder builder) {
        if (location != null) {
            /*
             * The getDisplayName() contract does not allow us to use it as an identifier. However current implementation
             * in super.getDisplayName() returns the filename provided that the input was a URI, URL, File or Path. Since
             * all those types are convertibles to URI, we can use (location != null) as a criterion.
             */
            builder.addTitleOrIdentifier(IOUtilities.filenameWithoutExtension(super.getDisplayName()), MetadataBuilder.Scope.ALL);
        }
    }

    /**
     * Registers only listeners for {@link WarningEvent}s on the assumption that most data stores
     * (at least the read-only ones) produce no change events.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullArgumentException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
    }
}
