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

import java.net.URI;
import java.util.logging.Logger;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.Version;
import org.opengis.parameter.ParameterDescriptorGroup;

/**
 * The provider of {@link CoverageJsonStore} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a {@code CoverageJsonStore}.
 *
 * Draft specification : https://github.com/opengeospatial/CoverageJSON
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see CoverageJsonStore
 */
@StoreMetadata(formatName    = CoverageJsonStoreProvider.NAME,
               fileSuffixes  = {"covjson"},
               capabilities  = Capability.READ,
               resourceTypes = {Aggregate.class, GridCoverageResource.class})
public class CoverageJsonStoreProvider extends DataStoreProvider {

    public static final String NAME = "CoverageJSON";
    /**
     * The MIME type for Coverage-JSON files.
     */
    private static final String MIME_TYPE = "application/vnd.cov+json";

    /**
     * The logger used by Coverage-JSON stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.internal.coveragejson");

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR = URIDataStore.Provider.descriptor(NAME);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final URI uri = connector.getStorageAs(URI.class);
        if (uri != null && uri.toString().toLowerCase().endsWith(".covjson")) {
            return new ProbeResult(true, MIME_TYPE, new Version("0.9"));
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        return new CoverageJsonStore(this, connector);
    }

    /**
     * {@return the logger used by CoverageJSON stores}.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
