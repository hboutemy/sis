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
package org.apache.sis.internal.sql.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.internal.storage.SubsetAdapter;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.FeatureSet;

/**
 *
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
abstract class SQLQueryAdapter implements SubsetAdapter.AdapterBuilder {

    private ColumnRef[] columns;
    private SortBy[] sorting;

    private CharSequence where;

    private final Supplier<? extends ANSIInterpreter> filterInterpreter;

    protected SQLQueryAdapter() {
        this(() -> new ANSIInterpreter());
    }

    protected SQLQueryAdapter(final Dialect dbDialect) {
        this(forDialect(dbDialect));
    }

    protected SQLQueryAdapter(Supplier<? extends ANSIInterpreter> filterInterpreter) {
        this.filterInterpreter = filterInterpreter;
    }

    /**
     * No-op implementation. SQL optimisation is dynamically applied through {@link StreamSQL}.
     * @param offset The offset to handle.
     * @return Input offset.
     */
    @Override
    public long offset(long offset) {
        return offset;
    }

    /**
     * No-op implementation. SQL optimisation is dynamically applied through {@link StreamSQL}.
     * @param limit The limit to handle.
     * @return Input limit.
     */
    @Override
    public long limit(long limit) {
        return limit;
    }

    @Override
    public final Filter filter(Filter filter) {
        try {
            final Object result = filter.accept(filterInterpreter.get(), null);
            if (ANSIInterpreter.isNonEmptyText(result)) {
                where = (CharSequence) result;
                return Filter.INCLUDE;
            }
        } catch (UnsupportedOperationException e) {
            // TODO: log
            where = null;
        }

        return filter;
    }

    @Override
    public boolean sort(SortBy[] comparison) {
        sorting = Arrays.copyOf(comparison, comparison.length);
        return false;
    }

    @Override
    public boolean select(List<SimpleQuery.Column> columns) {
        /* We've got a lot of trouble with current column API. It defines an expression and an alias, which allow to
         * infer output property type. However, it's very difficult with current methods to infer source columns used
         * for building output. Note that we could check if column expression is a property name or a literal, but if
         * any column is not one of those two, it means it uses unknown (for us) SQL columns, so we cannot filter
         * selected columns safely.
         */
        return false;
    }

    @Override
    public final Optional<FeatureSet> build() {
        if (isNoOp()) return Optional.empty();
        return Optional.of(create(where, sorting, columns));
    }

    protected abstract FeatureSet create(final CharSequence where, final SortBy[] sorting, final ColumnRef[] columns);

    private boolean isNoOp() {
        return (sorting == null || sorting.length < 1)
                && (columns == null || columns.length < 1)
                && (where == null || where.length() < 1);
    }

    static class Table extends SQLQueryAdapter {
        final org.apache.sis.internal.sql.feature.Table parent;
        public Table(org.apache.sis.internal.sql.feature.Table parent) {
            super(parent.createStatement().dialect);
            this.parent = parent;
        }

        @Override
        protected FeatureSet create(CharSequence where, SortBy[] sorting, ColumnRef[] columns) {
            // TODO: column information is lost for now. What should be done is factorize/sanitize feature set
            // implementations from this package to better handle SQL filtering.
            return new TableSubset(parent, sorting, where);
        }
    }

    /**
     * Give back a filter/expression interpreter for a given database dialect.
     * TODO: unify with {@link DialectMapping}.
     *
     * @param dialect Database dialect that must be produced by the interpreter. If null, {@link Dialect#ANSI} is used
     *                as a fallback.
     * @return A function able to give a fresh interpreter on demaind.
     */
    static Supplier<ANSIInterpreter> forDialect(final Dialect dialect) {
        switch (dialect) {
            case POSTGRESQL: return () -> new PostGISInterpreter();
            default: return () -> new ANSIInterpreter();
        }
    }
}
