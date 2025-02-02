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
package org.apache.sis.internal.storage.folder;

import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.aggregate.CoverageAggregator;


/**
 * A data store which may provide a more structured view of its components.
 * This is an experimental interface that may change in any future version.
 * Structure is inferred by {@link CoverageAggregator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
public interface UnstructuredAggregate extends Aggregate {
    /**
     * Returns a more structured (if possible) view of this resource.
     *
     * @return structured view. May be {@code this} if this method cannot do better than current resource.
     * @throws DataStoreException if an error occurred during the attempt to create a structured view.
     */
    Resource getStructuredView() throws DataStoreException;
}
