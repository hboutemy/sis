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
package org.apache.sis.internal.coveragejson;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.sis.internal.coveragejson.binding.Coverage;
import org.apache.sis.internal.coveragejson.binding.CoverageCollection;
import org.apache.sis.internal.coveragejson.binding.CoverageJsonObject;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.WritableAggregate;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;

/**
 * A data store backed by Coverage-JSON files.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageJsonStore extends DataStore implements WritableAggregate {

    /**
     * The {@link CoverageJsonStoreProvider#LOCATION} parameter value, or {@code null} if none.
     * This is used for information purpose only, not for actual reading operations.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * Same value than {@link #location} but as a path, or {@code null} if none.
     * Stored separately because conversion from path to URI back to path is not
     * looseness (relative paths become absolutes).
     */
    private final Path path;

    private boolean parsed;
    private final List<Resource> components = new ArrayList<>();

    CoverageJsonStore(CoverageJsonStoreProvider provider, StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        location = connector.getStorageAs(URI.class);
        path = connector.getStorageAs(Path.class);
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(URIDataStore.parameters(provider, location));
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addIdentifier(null, IOUtilities.filename(path), MetadataBuilder.Scope.ALL);
        return builder.buildAndFreeze();
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (!parsed) {
            parsed = true;
            if (Files.exists(path)) {
                try (Jsonb b = JsonbBuilder.create();
                     InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                    final CoverageJsonObject obj = b.fromJson(in, CoverageJsonObject.class);

                    if (obj instanceof Coverage) {
                        final Coverage coverage = (Coverage) obj;
                        components.add(new CoverageResource(this, coverage));

                    } else if (obj instanceof CoverageCollection) {
                        throw new UnsupportedOperationException("Coverage collection not supported yet.");
                    }

                } catch (Exception ex) {
                    throw new DataStoreException("Failed to parse coverage json object.", ex);
                }
            }
        }

        return Collections.unmodifiableList(components);
    }


    @Override
    public Resource add(Resource resource) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(Resource resource) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws DataStoreException {
    }
}
