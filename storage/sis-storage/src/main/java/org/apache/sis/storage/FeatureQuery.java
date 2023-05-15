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

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Function;
import java.io.Serializable;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.filter.SortByComparator;
import org.apache.sis.internal.filter.XPath;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.Optimization;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.resources.Vocabulary;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Operation;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.SortBy;
import org.opengis.filter.SortProperty;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Definition of filtering to apply for fetching a subset of {@link FeatureSet}.
 * This query mimics {@code SQL SELECT} statements using OGC Filter and Expressions.
 * Information stored in this query can be used directly with {@link java.util.stream.Stream} API.
 *
 * <h2>Terminology</h2>
 * This class uses relational database terminology:
 * <ul>
 *   <li>A <cite>selection</cite> is a filter choosing the features instances to include in the subset.
 *       In relational databases, a feature instances are mapped to table rows.</li>
 *   <li>A <cite>projection</cite> (not to be confused with map projection) is the set of feature properties to keep.
 *       In relational databases, feature properties are mapped to table columns.</li>
 * </ul>
 *
 * <h2>Optional values</h2>
 * All aspects of this query are optional and initialized to "none".
 * Unless otherwise specified, all methods accept a null argument or can return a null value, which means "none".
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public class FeatureQuery extends Query implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5841189659773611160L;

    /**
     * Sentinel limit value for queries of unlimited length.
     * This value applies to the {@link #limit} field.
     */
    private static final long UNLIMITED = -1;

    /**
     * The properties to retrieve, or {@code null} if all properties shall be included in the query.
     * In a database, "properties" are table columns.
     * Subset of columns is called <cite>projection</cite> in relational database terminology.
     *
     * @see #getProjection()
     * @see #setProjection(NamedExpression[])
     */
    private NamedExpression[] projection;

    /**
     * The filter for trimming feature instances.
     * In a database, "feature instances" are table rows.
     * Subset of rows is called <cite>selection</cite> in relational database terminology.
     *
     * @see #getSelection()
     * @see #setSelection(Filter)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Filter<? super Feature> selection;

    /**
     * The number of feature instances to skip from the beginning.
     * This is zero if there are no instances to skip.
     *
     * @see #getOffset()
     * @see #setOffset(long)
     * @see java.util.stream.Stream#skip(long)
     */
    private long skip;

    /**
     * The maximum number of feature instances contained in the {@code FeatureSet}.
     * This is {@link #UNLIMITED} if there is no limit.
     *
     * @see #getLimit()
     * @see #setLimit(long)
     * @see java.util.stream.Stream#limit(long)
     */
    private long limit;

    /**
     * The expressions to use for sorting the feature instances.
     *
     * @see #getSortBy()
     * @see #setSortBy(SortBy)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private SortBy<Feature> sortBy;

    /**
     * Hint used by resources to optimize returned features.
     * Different stores make use of vector tiles of different scales.
     * A {@code null} value means to query data at their full resolution.
     *
     * @see #getLinearResolution()
     * @see #setLinearResolution(Quantity)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Quantity<Length> linearResolution;

    /**
     * Creates a new query applying no filter.
     */
    public FeatureQuery() {
        limit = UNLIMITED;
    }

    /**
     * Sets the properties to retrieve by their names. This convenience method wraps the
     * given names in {@link ValueReference} expressions without alias and delegates to
     * {@link #setProjection(NamedExpression...)}.
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a property is duplicated.
     */
    @Override
    public void setProjection(final String... properties) {
        NamedExpression[] wrappers = null;
        if (properties != null) {
            final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
            wrappers = new NamedExpression[properties.length];
            for (int i=0; i<wrappers.length; i++) {
                final String p = properties[i];
                ArgumentChecks.ensureNonNullElement("properties", i, p);
                wrappers[i] = new NamedExpression(ff.property(p));
            }
        }
        setProjection(wrappers);
    }

    /**
     * Sets the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * This convenience method wraps the given expression in {@link NamedExpression}s without alias and
     * delegates to {@link #setProjection(NamedExpression...)}.
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a property is duplicated.
     */
    @SafeVarargs
    public final void setProjection(final Expression<? super Feature, ?>... properties) {
        NamedExpression[] wrappers = null;
        if (properties != null) {
            wrappers = new NamedExpression[properties.length];
            for (int i=0; i<wrappers.length; i++) {
                final Expression<? super Feature, ?> e = properties[i];
                ArgumentChecks.ensureNonNullElement("properties", i, e);
                wrappers[i] = new NamedExpression(e);
            }
        }
        setProjection(wrappers);
    }

    /**
     * Sets the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * A query column may use a simple or complex expression and an alias to create a new type of property
     * in the returned features.
     *
     * <p>This is equivalent to the column names in the {@code SELECT} clause of a SQL statement.
     * Subset of columns is called <cite>projection</cite> in relational database terminology.</p>
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a property or an alias is duplicated.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setProjection(NamedExpression... properties) {
        if (properties != null) {
            ArgumentChecks.ensureNonEmpty("properties", properties);
            properties = properties.clone();
            final Map<Object,Integer> uniques = new LinkedHashMap<>(Containers.hashMapCapacity(properties.length));
            for (int i=0; i<properties.length; i++) {
                final NamedExpression c = properties[i];
                ArgumentChecks.ensureNonNullElement("properties", i, c);
                Object key = (c.alias != null) ? c.alias : c.expression;
                final Integer p = uniques.putIfAbsent(key, i);
                if (p != null) {
                    if (key instanceof Expression) {
                        key = label((Expression) key);
                    }
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.DuplicatedQueryProperty_3, key, p, i));
                }
            }
        }
        this.projection = properties;
    }

    /**
     * Returns the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * This is the expressions specified in the last call to {@link #setProjection(NamedExpression[])}.
     * The default value is null.
     *
     * @return properties to retrieve, or {@code null} to retrieve all feature properties.
     */
    public NamedExpression[] getProjection() {
        return (projection != null) ? projection.clone() : null;
    }

    /**
     * Returns the properties to be stored in the target feature.
     */
    final NamedExpression[] getStoredProjection() {
        final NamedExpression[] stored = getProjection();
        if (stored != null) {
            int count = 0;
            for (final NamedExpression p : stored) {
                if (p.type == ProjectionType.STORED) {
                    stored[count++] = p;
                }
            }
            if (count != 0) {
                return ArraysExt.resize(stored, count);
            }
        }
        return null;
    }

    /**
     * Sets the approximate area of feature instances to include in the subset.
     * This convenience method creates a filter that checks if the bounding box
     * of the feature's {@code "sis:geometry"} property interacts with the given envelope.
     *
     * @param  domain  the approximate area of interest, or {@code null} if none.
     */
    @Override
    public void setSelection(final Envelope domain) {
        Filter<Feature> filter = null;
        if (domain != null) {
            final FilterFactory<Feature,Object,?> ff = DefaultFilterFactory.forFeatures();
            filter = ff.bbox(ff.property(AttributeConvention.GEOMETRY), domain);
        }
        setSelection(filter);
    }

    /**
     * Sets a filter for trimming feature instances.
     * Features that do not pass the filter are discarded.
     * Discarded features are not counted for the {@linkplain #setLimit(long) query limit}.
     *
     * @param  selection  the filter, or {@code null} if none.
     */
    public void setSelection(final Filter<? super Feature> selection) {
        this.selection = selection;
    }

    /**
     * Returns the filter for trimming feature instances.
     * This is the value specified in the last call to {@link #setSelection(Filter)}.
     * The default value is {@code null}, which means that no filtering is applied.
     *
     * @return the filter, or {@code null} if none.
     */
    public Filter<? super Feature> getSelection() {
        return selection;
    }

    /**
     * Sets the number of feature instances to skip from the beginning.
     * Offset and limit are often combined to obtain paging.
     * The offset cannot be negative.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#skip(long)} for more information.</p>
     *
     * @param  skip  the number of feature instances to skip from the beginning.
     */
    public void setOffset(final long skip) {
        ArgumentChecks.ensurePositive("skip", skip);
        this.skip = skip;
    }

    /**
     * Returns the number of feature instances to skip from the beginning.
     * This is the value specified in the last call to {@link #setOffset(long)}.
     * The default value is zero, which means that no features are skipped.
     *
     * @return the number of feature instances to skip from the beginning.
     */
    public long getOffset() {
        return skip;
    }

    /**
     * Removes any limit defined by {@link #setLimit(long)}.
     */
    public void setUnlimited() {
        limit = UNLIMITED;
    }

    /**
     * Set the maximum number of feature instances contained in the {@code FeatureSet}.
     * Offset and limit are often combined to obtain paging.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#limit(long)} for more information.</p>
     *
     * @param  limit  maximum number of feature instances contained in the {@code FeatureSet}.
     */
    public void setLimit(final long limit) {
        ArgumentChecks.ensurePositive("limit", limit);
        this.limit = limit;
    }

    /**
     * Returns the maximum number of feature instances contained in the {@code FeatureSet}.
     * This is the value specified in the last call to {@link #setLimit(long)}.
     *
     * @return maximum number of feature instances contained in the {@code FeatureSet}, or empty if none.
     */
    public OptionalLong getLimit() {
        return (limit >= 0) ? OptionalLong.of(limit) : OptionalLong.empty();
    }

    /**
     * Sets the expressions to use for sorting the feature instances.
     * {@code SortBy} objects are used to order the {@link Feature} instances returned by the {@link FeatureSet}.
     * {@code SortBy} clauses are applied in declaration order, like SQL.
     *
     * @param  properties  expressions to use for sorting the feature instances,
     *                     or {@code null} or an empty array if none.
     */
    @SafeVarargs
    public final void setSortBy(final SortProperty<Feature>... properties) {
        SortBy<Feature> sortBy = null;
        if (properties != null) {
            sortBy = SortByComparator.create(properties);
        }
        setSortBy(sortBy);
    }

    /**
     * Sets the expressions to use for sorting the feature instances.
     * {@code SortBy} objects are used to order the {@link Feature} instances returned by the {@link FeatureSet}.
     *
     * @param  sortBy  expressions to use for sorting the feature instances, or {@code null} if none.
     */
    public void setSortBy(final SortBy<Feature> sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Returns the expressions to use for sorting the feature instances.
     * This is the value specified in the last call to {@link #setSortBy(SortBy)}.
     *
     * @return expressions to use for sorting the feature instances, or {@code null} if none.
     */
    public SortBy<Feature> getSortBy() {
        return sortBy;
    }

    /**
     * Sets the desired spatial resolution of geometries.
     * This property is an optional hint; resources may ignore it.
     *
     * @param  linearResolution  desired spatial resolution, or {@code null} for full resolution.
     */
    public void setLinearResolution(final Quantity<Length> linearResolution) {
        this.linearResolution = linearResolution;
    }

    /**
     * Returns the desired spatial resolution of geometries.
     * A {@code null} value means that data are queried at their full resolution.
     *
     * @return  desired spatial resolution, or {@code null} for full resolution.
     */
    public Quantity<Length> getLinearResolution() {
        return linearResolution;
    }

    /**
     * Whether a property evaluated by a query is computed on the fly or stored.
     * By default, an expression is evaluated only once for each feature instance,
     * then the result is stored as a feature {@link Attribute} value.
     * But the same expression can also be wrapped in a feature {@link Operation}
     * and evaluated every times that the value is requested.
     *
     * <h2>Analogy with relational databases</h2>
     * The terminology used in this enumeration is close to the one used in relational database.
     * A <cite>projection</cite> is the set of feature properties to keep in the query results.
     * The projection may contain <cite>generated columns</cite>, which are specified in SQL by
     * {@code SQL GENERATED ALWAYS} statement, optionally with {@code STORED} or {@code VIRTUAL}
     * modifier.
     *
     * @version 1.4
     * @since   1.4
     */
    public enum ProjectionType {
        /**
         * The expression is evaluated exactly once when a feature instance is created,
         * and the result is stored as a feature attribute.
         * The feature property type will be {@link Attribute} and its value will be modifiable.
         * This is the default projection type.
         *
         * <h4>Feature instances in expression evaluation</h4>
         * The features given in calls to {@link Expression#apply(Object)} are instances from the
         * <em>source</em> {@link FeatureSet}, before filtering.
         */
        STORED,

        /*
         * The expression is evaluated every times that the property value is requested.
         * This projection type is similar to {@link #COMPUTING}, except that the features
         * given in calls to {@link Expression#apply(Object)} are the same instances than
         * the ones used by {@link #STORED}.
         *
         * <div class="note"><b>Note on naming:</b>
         * the {@code STORED} and {@code VIRTUAL} enumeration values are named according usage in SQL
         * {@code GENERATE ALWAYS} statement. Those two keywords work on columns in the source tables.
         * </div>
         *
         * <h4>Feature instances in expression evaluation</h4>
         * The combination of deferred calculation (like {@link #COMPUTING}) and usage of feature instances
         * from the <em>source</em> {@link FeatureSet} (like {@link #STORED}) may cause this projection type
         * to retain the source feature instances for a longer time than other types.
         *
         * @todo Waiting to see if there is a need for this type before to implement it.
         */
      // VIRTUAL,

        /**
         * The expression is evaluated every times that the property value is requested.
         * The feature property type will be {@link Operation}.
         * This projection type may be preferable to {@link #STORED} in the following circumstances:
         *
         * <ul>
         *   <li>The expression may produce different results every times that it is evaluated.</li>
         *   <li>The feature property should be a {@linkplain FeatureOperations#link link} to another attribute.</li>
         *   <li>Potentially expensive computation should be deferred until first needed.</li>
         *   <li>Computation result should not be stored in order to reduce memory usage.</li>
         * </ul>
         *
         * <h4>Feature instances in expression evaluation</h4>
         * The features given in calls to {@link Expression#apply(Object)} are instances from the <em>target</em>
         * {@link FeatureSet}, after filtering. The instances from the source {@code FeatureSet} are no longer
         * available when the expression is executed. Consequently, all fields that are necessary for computing
         * a {@code COMPUTING} field shall have been first copied in {@link #STORED} fields.
         *
         * <div class="note"><b>Note on naming:</b>
         * verb tense <i>-ing</i> instead of <i>-ed</i> is for emphasizing that the data used for computation
         * are current (filtered) data instead of past (original) data.</div>
         *
         * @see FeatureOperations#expression(Map, Function, AttributeType)
         */
        COMPUTING
    }

    /**
     * An expression to be retrieved by a {@code Query}, together with the name to assign to it.
     * {@code NamedExpression} specifies also if the expression should be evaluated exactly once
     * and its value stored, or evaluated every times that the value is requested.
     *
     * <h2>Analogy with relational databases</h2>
     * A {@code NamedExpression} instance can be understood as the definition of a column in a SQL database table.
     * In relational database terminology, subset of columns is called <cite>projection</cite>.
     * A projection is specified by a SQL {@code SELECT} statement, which maps to {@code NamedExpression} as below:
     *
     * <p>{@code SELECT} {@link #expression} {@code AS} {@link #alias}</p>
     *
     * Columns can be given to the {@link FeatureQuery#setProjection(NamedExpression[])} method.
     *
     * @version 1.4
     * @since   1.1
     */
    public static class NamedExpression implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 4547204390645035145L;

        /**
         * The literal, value reference or more complex expression to be retrieved by a {@code Query}.
         * Never {@code null}.
         */
        @SuppressWarnings("serial")
        public final Expression<? super Feature, ?> expression;

        /**
         * The name to assign to the expression result, or {@code null} if unspecified.
         */
        @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
        public final GenericName alias;

        /**
         * Whether the expression result should be stored or evaluated every times that it is requested.
         * A stored value will exist as a feature {@link Attribute}, while a virtual value will exist as
         * a feature {@link Operation}. The latter are commonly called "computed fields" and are equivalent
         * to SQL {@code GENERATED ALWAYS} keyword for columns.
         *
         * @since 1.4
         */
        public final ProjectionType type;

        /**
         * Creates a new stored column with the given expression and no name.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         */
        public NamedExpression(final Expression<? super Feature, ?> expression) {
            this(expression, (GenericName) null);
        }

        /**
         * Creates a new stored column with the given expression and the given name.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public NamedExpression(final Expression<? super Feature,?> expression, final GenericName alias) {
            this(expression, alias, ProjectionType.STORED);
        }

        /**
         * Creates a new stored column with the given expression and the given name.
         * This constructor creates a {@link org.opengis.util.LocalName} from the given string.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public NamedExpression(final Expression<? super Feature,?> expression, final String alias) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = (alias != null) ? Names.createLocalName(null, null, alias) : null;
            this.type = ProjectionType.STORED;
        }

        /**
         * Creates a new column with the given expression, the given name and the given projection type.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         * @param type        whether to create a feature {@link Attribute} or a feature {@link Operation}.
         *
         * @since 1.4
         */
        public NamedExpression(final Expression<? super Feature,?> expression, final GenericName alias, ProjectionType type) {
            ArgumentChecks.ensureNonNull("expression", expression);
            ArgumentChecks.ensureNonNull("type", type);
            this.expression = expression;
            this.alias = alias;
            this.type  = type;
        }

        /**
         * Returns a hash code value for this column.
         *
         * @return a hash code value.
         */
        @Override
        public int hashCode() {
            return 37 * expression.hashCode() + Objects.hashCode(alias) + type.hashCode();
        }

        /**
         * Compares this column with the given object for equality.
         *
         * @param  obj  the object to compare with this column.
         * @return whether the two objects are equal.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                final NamedExpression other = (NamedExpression) obj;
                return expression.equals(other.expression) && Objects.equals(alias, other.alias) && type == other.type;
            }
            return false;
        }

        /**
         * Returns a string representation of this column for debugging purpose.
         *
         * @return a string representation of this column.
         */
        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder("SELECT ");
            appendTo(buffer);
            return buffer.toString();
        }

        /**
         * Appends a string representation of this column in the given buffer.
         */
        final void appendTo(final StringBuilder buffer) {
            if (expression instanceof Literal<?,?>) {
                buffer.append('‘').append(((Literal<?,?>) expression).getValue()).append('’');
            } else if (expression instanceof ValueReference<?,?>) {
                buffer.append('“').append(((ValueReference<?,?>) expression).getXPath()).append('”');
            } else {
                buffer.append("=“").append(expression.getFunctionName()).append("”()");
            }
            if (type != ProjectionType.STORED) {
                buffer.append(' ').append(type);
            }
            if (alias != null) {
                buffer.append(" AS “").append(alias).append('”');
            }
        }
    }

    /**
     * Returns a label for the given expression for reporting to human (e.g. in exception messages).
     * This method uses the value reference (XPath) or literal value if applicable, truncated to an
     * arbitrary length.
     */
    private static String label(final Expression<?,?> expression) {
        final String text;
        if (expression instanceof Literal<?,?>) {
            text = String.valueOf(((Literal<?,?>) expression).getValue());
        } else if (expression instanceof ValueReference<?,?>) {
            text = ((ValueReference<?,?>) expression).getXPath();
        } else {
            return expression.getFunctionName().toString();
        }
        return CharSequences.shortSentence(text, 40).toString();
    }

    /**
     * Applies this query on the given feature set.
     * This method is invoked by the default implementation of {@link FeatureSet#subset(Query)}.
     * The default implementation executes the query using the default {@link java.util.stream.Stream} methods.
     * Queries executed by this method may not benefit from accelerations provided for example by databases.
     * This method should be used only as a fallback when the query cannot be executed natively
     * by {@link FeatureSet#subset(Query)}.
     *
     * <p>The returned {@code FeatureSet} does not cache the resulting {@code Feature} instances;
     * the query is processed on every call to the {@link FeatureSet#features(boolean)} method.</p>
     *
     * @param  source  the set of features to filter, sort or process.
     * @return a view over the given feature set containing only the filtered feature instances.
     * @throws DataStoreException if an error occurred during creation of the subset.
     *
     * @see FeatureSet#subset(Query)
     * @see CoverageQuery#execute(GridCoverageResource)
     *
     * @since 1.2
     */
    protected FeatureSet execute(final FeatureSet source) throws DataStoreException {
        ArgumentChecks.ensureNonNull("source", source);
        final FeatureQuery query = clone();
        if (query.selection != null) {
            final Optimization optimization = new Optimization();
            optimization.setFeatureType(source.getType());
            query.selection = optimization.apply(query.selection);
        }
        return new FeatureSubset(source, query);
    }

    /**
     * Returns the type of values evaluated by this query when executed on features of the given type.
     * If some expressions have no name, default names are computed as below:
     *
     * <ul>
     *   <li>If the expression is an instance of {@link ValueReference}, the name of the
     *       property referenced by the {@linkplain ValueReference#getXPath() x-path}.</li>
     *   <li>Otherwise the localized string "Unnamed #1" with increasing numbers.</li>
     * </ul>
     *
     * @param  valueType  the type of features to be evaluated by the expressions in this query.
     * @return type resulting from expressions evaluation (never null).
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     * @throws InvalidFilterValueException if this method cannot determine the result type of an expression
     *         in this query. It may be because that expression is backed by an unsupported implementation.
     */
    final FeatureType expectedType(final FeatureType valueType) {
        if (projection == null) {
            return valueType;           // All columns included: result is of the same type.
        }
        int unnamedNumber = 0;          // Sequential number for unnamed expressions.
        Set<String> names = null;       // Names already used, for avoiding collisions.
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName(valueType.getName());
        for (int column = 0; column < projection.length; column++) {
            final NamedExpression item = projection[column];
            /*
             * For each property, get the expected type (mandatory) and its name (optional).
             * A default name will be computed if no alias were explicitly given by user.
             */
            final Expression<? super Feature,?> expression = item.expression;
            final FeatureExpression<?,?> fex = FeatureExpression.castOrCopy(expression);
            final PropertyTypeBuilder resultType;
            if (fex == null || (resultType = fex.expectedType(valueType, ftb)) == null) {
                throw new InvalidFilterValueException(Resources.format(Resources.Keys.InvalidExpression_2,
                            expression.getFunctionName().toInternationalString(), column));
            }
            GenericName name = item.alias;
            if (name == null) {
                /*
                 * Build a list of aliases declared by the user, for making sure that we do not collide with them.
                 * No check for `GenericName` collision here because it was already verified by `setProjection(…)`.
                 * We may have collision of their `String` representations however, which is okay.
                 */
                if (names == null) {
                    names = new HashSet<>(Containers.hashMapCapacity(projection.length));
                    for (final NamedExpression p : projection) {
                        if (p.alias != null) {
                            names.add(p.alias.toString());
                        }
                    }
                }
                /*
                 * If the expression is a `ValueReference`, the `PropertyType` instance can be taken directly
                 * from the source feature (the Apache SIS implementation does just that). If the name is set,
                 * then we assume that it is correct. Otherwise we take the tip of the XPath.
                 */
                CharSequence text = null;
                if (expression instanceof ValueReference<?,?>) {
                    final GenericName current = resultType.getName();
                    if (current != null && names.add(current.toString())) {
                        continue;
                    }
                    String xpath = ((ValueReference<?,?>) expression).getXPath().trim();
                    xpath = xpath.substring(xpath.lastIndexOf(XPath.SEPARATOR) + 1);  // Works also if '/' is not found.
                    if (!(xpath.isEmpty() || names.contains(xpath))) {
                        text = xpath;
                    }
                }
                /*
                 * If we still have no name at this point, create a name like "Unnamed #1".
                 * Note that despite the use of `Vocabulary` resources, the name will be unlocalized
                 * (for easier programmatic use) because `GenericName` implementation is designed for
                 * providing localized names only if explicitly requested.
                 */
                if (text == null) do {
                    text = Vocabulary.formatInternational(Vocabulary.Keys.Unnamed_1, ++unnamedNumber);
                } while (!names.add(text.toString()));
                name = Names.createLocalName(null, null, text);
            }
            /*
             * If the attribute that we just added should be virtual, replace the attribute by an operation.
             * We need to keep the property name computed by `fex.expectedType(…)` for the operation result,
             * because that name is the name of the link to create if the operation is `ValueReference`.
             */
            if (item.type == ProjectionType.COMPUTING && resultType instanceof AttributeTypeBuilder<?>) {
                final var ab = (AttributeTypeBuilder<?>) resultType;
                final AttributeType<?> storedType = ab.build();
                if (ftb.properties().remove(resultType)) {
                    final var properties = Map.of(AbstractOperation.NAME_KEY, name);
                    ftb.addProperty(FeatureOperations.expression(properties, expression, storedType));
                }
            } else {
                resultType.setName(name);
            }
        }
        return ftb.build();
    }

    /**
     * Returns a clone of this query.
     *
     * @return a clone of this query.
     */
    @Override
    public FeatureQuery clone() {
        /*
         * Implementation note: no need to clone the arrays. It is safe to share the same array instances
         * because this class does not modify them and does not return them directly to the user.
         */
        try {
            return (FeatureQuery) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns a hash code value for this query.
     *
     * @return a hash value for this query.
     */
    @Override
    public int hashCode() {
        return 97 * Arrays.hashCode(projection) + 31 * Objects.hashCode(selection)
              + 7 * Objects.hashCode(sortBy) + Long.hashCode(limit ^ skip)
              + 3 * Objects.hashCode(linearResolution);
    }

    /**
     * Compares this query with the given object for equality.
     *
     * @param  obj  the object to compare with this query.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            final FeatureQuery other = (FeatureQuery) obj;
            return skip  == other.skip &&
                   limit == other.limit &&
                   Objects.equals(selection,        other.selection) &&
                   Arrays .equals(projection,       other.projection) &&
                   Objects.equals(sortBy,           other.sortBy) &&
                   Objects.equals(linearResolution, other.linearResolution);
        }
        return false;
    }

    /**
     * Returns a textual representation of this query for debugging purposes.
     * The default implementation returns a string that looks like an SQL Select query.
     *
     * @return textual representation of this query.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(80);
        sb.append("SELECT ");
        if (projection != null) {
            for (int i=0; i<projection.length; i++) {
                if (i != 0) sb.append(", ");
                projection[i].appendTo(sb);
            }
        } else {
            sb.append('*');
        }
        if (selection != null) {
            sb.append(" WHERE ").append(selection);
        }
        if (sortBy != null) {
            String separator = " ORDER BY ";
            for (final SortProperty<Feature> p : sortBy.getSortProperties()) {
                sb.append(separator);
                separator = ", ";
                sb.append(p.getValueReference().getXPath()).append(' ').append(p.getSortOrder());
            }
        }
        if (linearResolution != null) {
            sb.append(" RESOLUTION ").append(linearResolution);
        }
        if (limit != UNLIMITED) {
            sb.append(" LIMIT ").append(limit);
        }
        if (skip != 0) {
            sb.append(" OFFSET ").append(skip);
        }
        return sb.toString();
    }
}
