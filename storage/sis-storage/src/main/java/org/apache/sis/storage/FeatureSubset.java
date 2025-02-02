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
package org.apache.sis.storage;

import java.util.OptionalLong;
import java.util.stream.Stream;
import org.opengis.metadata.Metadata;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.internal.storage.Resources;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.filter.Expression;
import org.apache.sis.filter.Filter;
import org.apache.sis.internal.geoapi.filter.SortBy;


/**
 * The result of {@link FeatureQuery#execute(FeatureSet)} executed using Java {@link Stream} methods.
 * Queries executed by this class do not benefit from accelerations provided for example by databases.
 * This class should be used only as a fallback when the query cannot be executed natively by
 * {@link FeatureSet#subset(Query)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
final class FeatureSubset extends AbstractFeatureSet {
    /**
     * The set of feature instances to filter, sort or process.
     */
    private final FeatureSet source;

    /**
     * The query for filtering the source set of features.
     */
    private final FeatureQuery query;

    /**
     * The type of features in this set. May or may not be the same as {@link #source}.
     * This is computed when first needed.
     */
    private DefaultFeatureType resultType;

    /**
     * Creates a new set of features by filtering the given set using the given query.
     * This given query is stored as-is (it is not cloned neither optimized).
     */
    FeatureSubset(final FeatureSet source, final FeatureQuery query) {
        super(source);
        this.source = source;
        this.query = query;
    }

    /**
     * Creates metadata about this subset.
     * It includes information about the complete feature set.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addDefaultMetadata(this, listeners);
        builder.addLineage(Resources.formatInternational(Resources.Keys.UnfilteredData));
        builder.addProcessDescription(Resources.formatInternational(Resources.Keys.SubsetQuery_1, StoreUtilities.getLabel(source)));
        builder.addSource(source.getMetadata());
        return builder.build();
    }

    /**
     * Returns a description of properties that are common to all features in this dataset.
     */
    @Override
    public synchronized DefaultFeatureType getType() throws DataStoreException {
        if (resultType == null) {
            final DefaultFeatureType type = source.getType();
            try {
                resultType = query.expectedType(type);
            } catch (IllegalArgumentException e) {
                throw new DataStoreContentException(Resources.forLocale(listeners.getLocale())
                        .getString(Resources.Keys.CanNotDeriveTypeFromFeature_1, type.getName()), e);
            }
        }
        return resultType;
    }

    /**
     * Returns a stream of all features contained in this dataset.
     */
    @Override
    public Stream<AbstractFeature> features(final boolean parallel) throws DataStoreException {
        Stream<AbstractFeature> stream = source.features(parallel);
        /*
         * Apply filter.
         */
        final Filter<? super AbstractFeature> selection = query.getSelection();
        if (selection != null && !selection.equals(Filter.include())) {
            stream = stream.filter(selection);
        }
        /*
         * Apply sorting.
         */
        final SortBy<AbstractFeature> sortBy = query.getSortBy();
        if (sortBy != null) {
            stream = stream.sorted(sortBy);
        }
        /*
         * Apply offset.
         */
        final long offset = query.getOffset();
        if (offset > 0) {
            stream = stream.skip(offset);
        }
        /*
         * Apply limit.
         */
        final OptionalLong limit = query.getLimit();
        if (limit.isPresent()) {
            stream = stream.limit(limit.getAsLong());
        }
        /*
         * Transform feature instances.
         * Note: "projection" here is in relational database sense, not map projection.
         */
        final FeatureQuery.NamedExpression[] projection = query.getStoredProjection();
        if (projection != null) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Expression<? super AbstractFeature,?>[] expressions = new Expression[projection.length];
            for (int i=0; i<expressions.length; i++) {
                expressions[i] = projection[i].expression;
            }
            final DefaultFeatureType type = getType();
            final String[] names = FeatureUtilities.getNames(type.getProperties(false));
            stream = stream.map(t -> {
                final AbstractFeature f = type.newInstance();
                for (int i=0; i < expressions.length; i++) {
                    f.setPropertyValue(names[i], expressions[i].apply(t));
                }
                return f;
            });
        }
        return stream;
    }
}
