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
package org.apache.sis.internal.storage.image;

import java.net.URL;
import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.storage.CoverageReadConsistency;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;


/**
 * Test consistency of read operations in random domains.
 * Assuming that the code reading the full extent is correct, this class can detect some bugs
 * in the code reading sub-regions or applying sub-sampling. This assumption is reasonable if
 * we consider that the code reading the full extent is usually simpler than the code reading
 * a subset of data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 */
public final class SelfConsistencyTest extends CoverageReadConsistency {
    /**
     * The store used for the test, opened only once.
     */
    private static WorldFileStore store;

    /**
     * Opens the test file to be used for all tests.
     *
     * @throws IOException if an error occurred while opening the file.
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @BeforeClass
    public static void openFile() throws IOException, DataStoreException {
        final URL url = WorldFileStoreTest.class.getResource("gradient.png");
        assertNotNull("Test file not found.", url);
        store = new WorldFileStore(null, new StorageConnector(url));
    }

    /**
     * Closes the test file used by all tests.
     *
     * @throws DataStoreException if an error occurred while closing the file.
     */
    @AfterClass
    public static void closeFile() throws DataStoreException {
        final WorldFileStore s = store;
        if (s != null) {
            store = null;       // Clear first in case of failure.
            s.close();
        }
    }

    /**
     * Creates a new test case.
     *
     * @throws DataStoreException if an error occurred while fetching the first image.
     */
    public SelfConsistencyTest() throws DataStoreException {
        super(store.components().iterator().next());
    }
}
