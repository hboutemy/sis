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
package org.apache.sis.referencing.operation.transform;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Constructor;
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.IncommensurableException;

import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

import org.apache.sis.io.wkt.Parser;
import org.apache.sis.internal.util.URLs;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.j2d.ParameterizedAffine;
import org.apache.sis.internal.referencing.provider.AbstractProvider;
import org.apache.sis.internal.referencing.provider.VerticalOffset;
import org.apache.sis.internal.referencing.provider.GeographicToGeocentric;
import org.apache.sis.internal.referencing.provider.GeocentricToGeographic;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.system.Reflect;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


/**
 * Low level factory for creating {@linkplain AbstractMathTransform math transforms}.
 * The objects created by this factory do not know what the source and target coordinate systems mean.
 * Because of this low semantic value, high level GIS applications usually do not need to use this factory directly.
 * They can use the static convenience methods in the {@link org.apache.sis.referencing.CRS}
 * or {@link MathTransforms} classes instead.
 *
 *
 * <h2>Standard parameters</h2>
 * {@code MathTransform} instances are created from {@linkplain DefaultParameterValueGroup parameter values}.
 * The parameters expected by each operation available in a default Apache SIS installation is
 * <a href="https://sis.apache.org/tables/CoordinateOperationMethods.html">listed here</a>.
 * The set of parameters varies for each operation or projection, but the following can be considered typical:
 *
 * <ul>
 *   <li>A <cite>semi-major</cite> and <cite>semi-minor</cite> axis length in metres.</li>
 *   <li>A <cite>central meridian</cite> and <cite>latitude of origin</cite> in decimal degrees.</li>
 *   <li>A <cite>scale factor</cite>, which default to 1.</li>
 *   <li>A <cite>false easting</cite> and <cite>false northing</cite> in metres, which default to 0.</li>
 * </ul>
 *
 * <p>Each descriptor has many aliases, and those aliases may vary between different projections.
 * For example, the <cite>false easting</cite> parameter is usually called {@code "false_easting"}
 * by OGC, while EPSG uses various names like <cite>"False easting"</cite> or <cite>"Easting at
 * false origin"</cite>.</p>
 *
 * <h2>Dynamic parameters</h2>
 * A few non-standard parameters are defined for compatibility reasons,
 * but delegates their work to standard parameters. Those dynamic parameters are not listed in the
 * {@linkplain DefaultParameterValueGroup#values() parameter values}.
 * Dynamic parameters are:
 *
 * <ul>
 *   <li>{@code "earth_radius"}, which copy its value to the {@code "semi_major"} and
 *       {@code "semi_minor"} parameter values.</li>
 *   <li>{@code "inverse_flattening"}, which compute the {@code "semi_minor"} value from
 *       the {@code "semi_major"} parameter value.</li>
 *   <li>{@code "standard_parallel"} expecting an array of type {@code double[]}, which copy
 *       its elements to the {@code "standard_parallel_1"} and {@code "standard_parallel_2"}
 *       parameter scalar values.</li>
 * </ul>
 *
 * <p>The main purpose of those dynamic parameters is to support some less commonly used conventions
 * without duplicating the most commonly used conventions. The alternative ways are used in netCDF
 * files for example, which often use spherical models instead of ellipsoidal ones.</p>
 *
 *
 * <h2><a id="Obligation">Mandatory and optional parameters</a></h2>
 * Parameters are flagged as either <cite>mandatory</cite> or <cite>optional</cite>.
 * A parameter may be mandatory and still have a default value. In the context of this package, "mandatory"
 * means that the parameter is an essential part of the projection defined by standards.
 * Such mandatory parameters will always appears in any <cite>Well Known Text</cite> (WKT) formatting,
 * even if not explicitly set by the user. For example, the central meridian is typically a mandatory
 * parameter with a default value of 0° (the Greenwich meridian).
 *
 * <p>Optional parameters, on the other hand, are often non-standard extensions.
 * They will appear in WKT formatting only if the user defined explicitly a value which is different than the
 * default value.</p>
 *
 *
 * <h2>Operation methods discovery</h2>
 * {@link OperationMethod} describes all the parameters expected for instantiating a particular kind of
 * math transform. The set of operation methods known to this factory can be obtained in two ways:
 *
 * <ul>
 *   <li>{@linkplain #DefaultMathTransformFactory(Iterable) specified explicitly at construction time}, or</li>
 *   <li>{@linkplain #DefaultMathTransformFactory() discovered by scanning the classpath}.</li>
 * </ul>
 *
 * The default way is to scan the classpath. See {@link MathTransformProvider} for indications about how to add
 * custom coordinate operation methods in a default Apache SIS installation.
 *
 *
 * <h2>Thread safety</h2>
 * This class is safe for multi-thread usage if all referenced {@code OperationMethod} instances are thread-safe.
 * There is typically only one {@code MathTransformFactory} instance for the whole application.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @version 1.4
 *
 * @see MathTransformProvider
 * @see AbstractMathTransform
 *
 * @since 0.6
 */
public class DefaultMathTransformFactory extends AbstractFactory implements MathTransformFactory, Parser {
    /*
     * NOTE FOR JAVADOC WRITER:
     * The "method" word is ambiguous here, because it can be "Java method" or "coordinate operation method".
     * In this class, we reserve the "method" word for "coordinate operation method" as much as possible.
     * For Java methods, we rather use "constructor" or "function".
     */

    /**
     * Minimal precision of ellipsoid semi-major and semi-minor axis lengths, in metres.
     * If the length difference between the axis of two ellipsoids is greater than this threshold,
     * we will report a mismatch. This is used for logging purpose only and do not have any impact
     * on the {@code MathTransform} objects to be created by this factory.
     */
    private static final double ELLIPSOID_PRECISION = Formulas.LINEAR_TOLERANCE;

    /**
     * The constructor for WKT parsers, fetched when first needed. The WKT parser is defined in the
     * same module than this class, so we will hopefully not have security issues.  But we have to
     * use reflection because the parser class is not yet public (because we do not want to commit
     * its API yet).
     */
    private static volatile Constructor<? extends Parser> parserConstructor;

    /**
     * All methods specified at construction time or found on the classpath.
     * If the iterable is an instance of {@link ServiceLoader}, then it will
     * be reloaded when {@link #reload()} is invoked.
     *
     * <p>All uses of this field shall be synchronized on {@code methods}.</p>
     */
    private final Iterable<? extends OperationMethod> methods;

    /**
     * The methods by name, cached for faster lookup and for avoiding some
     * synchronizations on {@link #methods} and {@link #pool}.
     */
    private final ConcurrentMap<String, OperationMethod> methodsByName;

    /**
     * The methods by type. All uses of this map shall be synchronized on {@code methodsByType}.
     *
     * <h4>Implementation note</h4>
     * We do not use a concurrent map here because the number of entries is expected to be very small
     * (about 2 entries), which make concurrent algorithms hardly efficient. Furthermore, this map is
     * not used often.
     */
    private final Map<Class<?>, OperationMethodSet> methodsByType;

    /**
     * The last coordinate operation method used by a {@code create(…)} constructor.
     */
    private final ThreadLocal<OperationMethod> lastMethod;

    /**
     * The math transforms created so far. This pool is used in order to return instances of existing
     * math transforms when possible. If {@code null}, then no pool should be used. A null value is
     * preferable when the transforms are known to be short-lived, for avoiding the cost of caching them.
     */
    private final WeakHashSet<MathTransform> pool;

    /**
     * The <cite>Well Known Text</cite> parser for {@code MathTransform} instances.
     * This parser is not thread-safe, so we need to prevent two threads from using
     * the same instance at the same time.
     */
    private final AtomicReference<Parser> parser;

    /**
     * The factory with opposite caching factory, or {@code null} if not yet created.
     *
     * @see #caching(boolean)
     */
    private DefaultMathTransformFactory oppositeCachingPolicy;

    /**
     * The default factory instance.
     */
    private static final DefaultMathTransformFactory INSTANCE = new DefaultMathTransformFactory();

    /**
     * Returns the default provider of {@code MathTransform} instances.
     * This is the factory used by the Apache SIS library when no non-null
     * {@link MathTransformFactory} has been explicitly specified.
     * This method can be invoked directly, or indirectly through
     * {@code ServiceLoader.load(MathTransformFactory.class)}.
     *
     * @return the default provider of math transforms.
     *
     * @see java.util.ServiceLoader
     * @since 1.4
     */
    public static DefaultMathTransformFactory provider() {
        return INSTANCE;
    }

    /**
     * Creates a new factory which will discover operation methods with a {@link ServiceLoader}.
     * The {@link OperationMethod} implementations shall be listed in the following file:
     *
     * <pre class="text">META-INF/services/org.opengis.referencing.operation.OperationMethod</pre>
     *
     * {@code DefaultMathTransformFactory} parses the above-cited files in all JAR files in order to find all available
     * operation methods. By default, only operation methods that implement the {@link MathTransformProvider} interface
     * can be used by the {@code create(…)} methods in this class.
     *
     * @see #provider()
     * @see #reload()
     */
    public DefaultMathTransformFactory() {
        this(ServiceLoader.load(OperationMethod.class, Reflect.getContextClassLoader()));
    }

    /**
     * Creates a new factory which will use the given operation methods. The given iterable is stored by reference —
     * its content is <strong>not</strong> copied — in order to allow deferred {@code OperationMethod} constructions.
     * Note that by default, only operation methods that implement the {@link MathTransformProvider} interface can be
     * used by the {@code create(…)} methods in this class.
     *
     * <h4>Requirements</h4>
     * <ul>
     *   <li>The given iterable should not contain duplicated elements.</li>
     *   <li>The given iterable shall be stable: all elements returned by the first iteration must also be
     *       returned by any subsequent iterations, unless {@link #reload()} has been invoked.</li>
     *   <li>{@code OperationMethod} instances should also implement {@link MathTransformProvider}.</li>
     *   <li>All {@code OperationMethod} instances shall be thread-safe.</li>
     *   <li>The {@code Iterable} itself does not need to be thread-safe since all usages will be synchronized as below:
     *
     *       {@snippet lang="java" :
     *           synchronized (methods) {
     *               for (OperationMethod method : methods) {
     *                   // Use the method here.
     *               }
     *           }
     *           }
     *   </li>
     * </ul>
     *
     * @param  methods  the operation methods to use, stored by reference (not copied).
     */
    public DefaultMathTransformFactory(final Iterable<? extends OperationMethod> methods) {
        ArgumentChecks.ensureNonNull("methods", methods);
        this.methods  = methods;
        methodsByName = new ConcurrentHashMap<>();
        methodsByType = new IdentityHashMap<>();
        lastMethod    = new ThreadLocal<>();
        pool          = new WeakHashSet<>(MathTransform.class);
        parser        = new AtomicReference<>();
    }

    /**
     * Creates a new factory with the same configuration than given factory but without caching.
     */
    private DefaultMathTransformFactory(final DefaultMathTransformFactory parent) {
        methods       = parent.methods;
        methodsByName = parent.methodsByName;
        methodsByType = parent.methodsByType;
        lastMethod    = new ThreadLocal<>();
        pool          = null;
        parser        = parent.parser;
        oppositeCachingPolicy = parent;
    }

    /**
     * Returns a factory for the same transforms than this factory, but with caching potentially disabled.
     * By default, {@code DefaultMathTransformFactory} caches the {@link MathTransform} instances for sharing
     * existing instances when transforms are created many times with the same set of parameters.
     * However, this caching may be unnecessarily costly when the transforms to create are known to be short lived.
     * This method allows to get a factory better suited for short-lived objects.
     *
     * <p>This method does not modify the state of this factory. Instead, different factory instances for the
     * different caching policy are returned.</p>
     *
     * @param  enabled  whether caching should be enabled.
     * @return a factory for the given caching policy.
     *
     * @since 1.1
     */
    public DefaultMathTransformFactory caching(final boolean enabled) {
        if (enabled) {
            return this;
        }
        synchronized (this) {
            if (oppositeCachingPolicy == null) {
                oppositeCachingPolicy = new NoCache(this);
            }
            return oppositeCachingPolicy;
        }
    }

    /**
     * Accessor for {@link NoCache} implementation.
     */
    final DefaultMathTransformFactory oppositeCachingPolicy() {
        return oppositeCachingPolicy;
    }

    /**
     * A factory performing no caching.
     * This factory shares the same operation methods than the parent factory.
     */
    private static final class NoCache extends DefaultMathTransformFactory {
        /** Creates a new factory with the same configuration than given factory. */
        NoCache(final DefaultMathTransformFactory parent) {
            super(parent);
        }

        /** Returns a factory for the same transforms but given caching policy. */
        @Override public DefaultMathTransformFactory caching(final boolean enabled) {
            return enabled ? oppositeCachingPolicy() : this;
        }

        /** Notifies parent factory that the set of operation methods may have changed. */
        @Override public void reload() {
            oppositeCachingPolicy().reload();
        }
    }

    /**
     * Returns a set of available methods for coordinate operations of the given type.
     * The {@code type} argument can be used for filtering the kind of operations described by the returned
     * {@code OperationMethod}s. The argument is usually (but not restricted to) one of the following types:
     *
     * <ul>
     *   <li>{@link org.opengis.referencing.operation.Transformation}
     *       for coordinate operations described by empirically derived parameters.</li>
     *   <li>{@link org.opengis.referencing.operation.Conversion}
     *       for coordinate operations described by definitions.</li>
     *   <li>{@link org.opengis.referencing.operation.Projection}
     *       for conversions from geodetic latitudes and longitudes to plane (map) coordinates.</li>
     *   <li>{@link SingleOperation} for all coordinate operations.</li>
     * </ul>
     *
     * The returned set may conservatively contain more {@code OperationMethod} elements than requested
     * if this {@code MathTransformFactory} does not support filtering by the given type.
     *
     * @param  type  <code>{@linkplain SingleOperation}.class</code> for fetching all operation methods,
     *               <code>{@linkplain org.opengis.referencing.operation.Projection}.class</code> for
     *               fetching only map projection methods, <i>etc</i>.
     * @return methods available in this factory for coordinate operations of the given type.
     *
     * @see #getDefaultParameters(String)
     * @see #createParameterizedTransform(ParameterValueGroup, Context)
     * @see DefaultOperationMethod#getOperationType()
     */
    @Override
    public Set<OperationMethod> getAvailableMethods(final Class<? extends SingleOperation> type) {
        ArgumentChecks.ensureNonNull("type", type);
        OperationMethodSet set;
        synchronized (methodsByType) {
            set = methodsByType.get(type);
        }
        if (set == null) {
            /*
             * Implementation note: we are better to avoid holding a lock on `methods` and `methodsByType`
             * at the same time because the `methods` iterator could be a user's implementation which callback
             * this factory.
             */
            synchronized (methods) {
                set = new OperationMethodSet(type, methods);
            }
            final OperationMethodSet previous;
            synchronized (methodsByType) {
                previous = methodsByType.putIfAbsent(type, set);
            }
            if (previous != null) {
                set = previous;
            }
        }
        return set;
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either
     * a method {@linkplain DefaultOperationMethod#getName() name} (e.g. <cite>"Transverse Mercator"</cite>)
     * or one of its {@linkplain DefaultOperationMethod#getIdentifiers() identifiers} (e.g. {@code "EPSG:9807"}).
     *
     * <p>The search is case-insensitive. Comparisons against method names can be
     * {@linkplain DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.</p>
     *
     * <p>If more than one method match the given identifier, then the first (according iteration order)
     * non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  identifier  the name or identifier of the operation method to search.
     * @return the coordinate operation method for the given name or identifier.
     * @throws NoSuchIdentifierException if there is no operation method registered for the specified identifier.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     */
    public OperationMethod getOperationMethod(String identifier) throws NoSuchIdentifierException {
        ArgumentChecks.ensureNonEmpty("identifier", identifier = identifier.strip());
        OperationMethod method = methodsByName.get(identifier);
        if (method == null) {
            synchronized (methods) {
                method = CoordinateOperations.getOperationMethod(methods, identifier);
            }
            if (method == null) {
                throw new NoSuchIdentifierException(Resources.format(
                        Resources.Keys.NoSuchOperationMethod_2, identifier, URLs.OPERATION_METHODS), identifier);
            }
            /*
             * Remember the method we just found, for faster check next time.
             */
            final OperationMethod previous = methodsByName.putIfAbsent(identifier.intern(), method);
            if (previous != null) {
                method = previous;
            }
        }
        return method;
    }

    /**
     * Returns the default parameter values for a math transform using the given operation method.
     * The {@code method} argument is the name of any {@code OperationMethod} instance returned by
     * <code>{@link #getAvailableMethods(Class) getAvailableMethods}({@linkplain SingleOperation}.class)</code>.
     * Valid names are <a href="https://sis.apache.org/tables/CoordinateOperationMethods.html">listed here</a>.
     *
     * <p>This function creates new parameter instances at every call.
     * Parameters are intended to be modified by the user before to be given to the
     * {@link #createParameterizedTransform createParameterizedTransform(…)} method.</p>
     *
     * @param  method  the case insensitive name of the coordinate operation method to search for.
     * @return a new group of parameter values for the {@code OperationMethod} identified by the given name.
     * @throws NoSuchIdentifierException if there is no method registered for the given name or identifier.
     *
     * @see #getAvailableMethods(Class)
     * @see #createParameterizedTransform(ParameterValueGroup, Context)
     * @see AbstractMathTransform#getParameterValues()
     */
    @Override
    public ParameterValueGroup getDefaultParameters(final String method) throws NoSuchIdentifierException {
        return getOperationMethod(method).getParameters().createValue();
    }

    /**
     * Creates a transform from a group of parameters.
     * The set of expected parameters varies for each operation.
     *
     * @param  parameters  the parameter values. The {@linkplain ParameterDescriptorGroup#getName() parameter group name}
     *         shall be the name of the desired {@linkplain DefaultOperationMethod operation method}.
     * @return the transform created from the given parameters.
     * @throws NoSuchIdentifierException if there is no method for the given parameter group name.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @deprecated Replaced by {@link #createParameterizedTransform(ParameterValueGroup, Context)}
     *             where the {@code Context} argument can be null.
     */
    @Override
    @Deprecated(since="0.7")
    public MathTransform createParameterizedTransform(final ParameterValueGroup parameters)
            throws NoSuchIdentifierException, FactoryException
    {
        return createParameterizedTransform(parameters, null);
    }

    /**
     * Source and target coordinate systems for which a new parameterized transform is going to be used.
     * {@link DefaultMathTransformFactory} uses this information for:
     *
     * <ul>
     *   <li>Completing some parameters if they were not provided. In particular, the {@linkplain #getSourceEllipsoid()
     *       source ellipsoid} can be used for providing values for the {@code "semi_major"} and {@code "semi_minor"}
     *       parameters in map projections.</li>
     *   <li>{@linkplain CoordinateSystems#swapAndScaleAxes Swapping and scaling axes} if the source or the target
     *       coordinate systems are not {@linkplain AxesConvention#NORMALIZED normalized}.</li>
     * </ul>
     *
     * This class does <strong>not</strong> handle change of
     * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#getPrimeMeridian() prime meridian}
     * or anything else related to datum. Datum changes have dedicated {@link OperationMethod},
     * for example <cite>"Longitude rotation"</cite> (EPSG:9601) for changing the prime meridian.
     *
     * <h2>Scope</h2>
     * Instances of this class should be short-lived
     * (they exist only the time needed for creating a {@link MathTransform})
     * and should not be shared (because they provide no immutability guarantees).
     * This class is not thread-safe.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.3
     * @since   0.7
     */
    @SuppressWarnings("serial")         // All field values are usually serializable instances.
    public static class Context implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -239563539875674709L;

        /**
         * Coordinate system of the source or target points.
         */
        private CoordinateSystem sourceCS, targetCS;

        /**
         * The ellipsoid of the source or target ellipsoidal coordinate system, or {@code null} if it does not apply.
         * Valid only if {@link #sourceCS} or {@link #targetCS} is an instance of {@link EllipsoidalCS}.
         */
        private Ellipsoid sourceEllipsoid, targetEllipsoid;

        /**
         * The provider that created the parameterized {@link MathTransform} instance, or {@code null}
         * if this information does not apply. This field is used for transferring information between
         * {@code createParameterizedTransform(…)} and {@code swapAndScaleAxes(…)}.
         */
        private OperationMethod provider;

        /**
         * The parameters actually used.
         *
         * @see #getCompletedParameters()
         */
        private ParameterValueGroup parameters;

        /**
         * Names of parameters which have been inferred from context.
         *
         * @see #getContextualParameters()
         */
        private final Map<String,Boolean> contextualParameters;

        /**
         * Creates a new context with all properties initialized to {@code null}.
         */
        public Context() {
            contextualParameters = new HashMap<>();
        }

        /**
         * Sets the source coordinate system to the given value.
         * The source ellipsoid is unconditionally set to {@code null}.
         *
         * @param  cs  the coordinate system to set as the source (can be {@code null}).
         */
        public void setSource(final CoordinateSystem cs) {
            sourceCS = cs;
            sourceEllipsoid = null;
        }

        /**
         * Sets the source coordinate system and related ellipsoid to the components of given CRS.
         * The {@link Ellipsoid}, fetched from the geodetic datum, is often used together with an {@link EllipsoidalCS},
         * but not necessarily. The geodetic CRS may also be associated with a spherical or Cartesian coordinate system,
         * and the ellipsoid information may still be needed even with those non-ellipsoidal coordinate systems.
         *
         * <p><strong>This method is not for datum shifts.</strong>
         * All datum information other than the ellipsoid are ignored.</p>
         *
         * @param  crs  the coordinate system and ellipsoid to set as the source, or {@code null}.
         *
         * @since 1.3
         */
        public void setSource(final GeodeticCRS crs) {
            if (crs != null) {
                sourceCS = crs.getCoordinateSystem();
                sourceEllipsoid = crs.getDatum().getEllipsoid();
            } else {
                sourceCS = null;
                sourceEllipsoid = null;
            }
        }

        /**
         * Sets the target coordinate system to the given value.
         * The target ellipsoid is unconditionally set to {@code null}.
         *
         * @param  cs  the coordinate system to set as the target (can be {@code null}).
         */
        public void setTarget(final CoordinateSystem cs) {
            targetCS = cs;
            targetEllipsoid = null;
        }

        /**
         * Sets the target coordinate system and related ellipsoid to the components of given CRS.
         * The {@link Ellipsoid}, fetched from the geodetic datum, is often used together with an {@link EllipsoidalCS},
         * but not necessarily. The geodetic CRS may also be associated with a spherical or Cartesian coordinate system,
         * and the ellipsoid information may still be needed even with those non-ellipsoidal coordinate systems.
         *
         * <p><strong>This method is not for datum shifts.</strong>
         * All datum information other than the ellipsoid are ignored.</p>
         *
         * @param  crs  the coordinate system and ellipsoid to set as the target, or {@code null}.
         *
         * @since 1.3
         */
        public void setTarget(final GeodeticCRS crs) {
            if (crs != null) {
                targetCS = crs.getCoordinateSystem();
                targetEllipsoid = crs.getDatum().getEllipsoid();
            } else {
                targetCS = null;
                targetEllipsoid = null;
            }
        }

        /**
         * Returns the source coordinate system, or {@code null} if unspecified.
         *
         * @return the source coordinate system, or {@code null}.
         */
        public CoordinateSystem getSourceCS() {
            return sourceCS;
        }

        /**
         * Returns the ellipsoid of the source ellipsoidal coordinate system, or {@code null} if it does not apply.
         * This information is valid only if {@link #getSourceCS()} returns an instance of {@link EllipsoidalCS}.
         *
         * @return the ellipsoid of the source ellipsoidal coordinate system, or {@code null} if it does not apply.
         */
        public Ellipsoid getSourceEllipsoid() {
            return sourceEllipsoid;
        }

        /**
         * Returns the target coordinate system, or {@code null} if unspecified.
         *
         * @return the target coordinate system, or {@code null}.
         */
        public CoordinateSystem getTargetCS() {
            return targetCS;
        }

        /**
         * Returns the ellipsoid of the target ellipsoidal coordinate system, or {@code null} if it does not apply.
         * This information is valid only if {@link #getTargetCS()} returns an instance of {@link EllipsoidalCS}.
         *
         * @return the ellipsoid of the target ellipsoidal coordinate system, or {@code null} if it does not apply.
         */
        public Ellipsoid getTargetEllipsoid() {
            return targetEllipsoid;
        }

        /**
         * Returns the matrix that represent the affine transform to concatenate before or after
         * the parameterized transform. The {@code role} argument specifies which matrix is desired:
         *
         * <ul class="verbose">
         *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#NORMALIZATION
         *       NORMALIZATION} for the conversion from the {@linkplain #getSourceCS() source coordinate system} to
         *       a {@linkplain AxesConvention#NORMALIZED normalized} coordinate system, usually with
         *       (<var>longitude</var>, <var>latitude</var>) axis order in degrees or
         *       (<var>easting</var>, <var>northing</var>) in metres.
         *       This normalization needs to be applied <em>before</em> the parameterized transform.</li>
         *
         *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#DENORMALIZATION
         *       DENORMALIZATION} for the conversion from a normalized coordinate system to the
         *       {@linkplain #getTargetCS() target coordinate system}, for example with
         *       (<var>latitude</var>, <var>longitude</var>) axis order.
         *       This denormalization needs to be applied <em>after</em> the parameterized transform.</li>
         *
         *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#INVERSE_NORMALIZATION INVERSE_NORMALIZATION} and
         *       {@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#INVERSE_DENORMALIZATION INVERSE_DENORMALIZATION}
         *       are also supported but rarely used.</li>
         * </ul>
         *
         * This method is invoked by {@link DefaultMathTransformFactory#swapAndScaleAxes(MathTransform, Context)}.
         * Users an override this method if they need to customize the normalization process.
         *
         * @param  role  whether the normalization or denormalization matrix is desired.
         * @return the requested matrix, or {@code null} if this {@code Context} has no information about the coordinate system.
         * @throws FactoryException if an error occurred while computing the matrix.
         *
         * @see DefaultMathTransformFactory#createAffineTransform(Matrix)
         * @see DefaultMathTransformFactory#createParameterizedTransform(ParameterValueGroup, Context)
         */
        @SuppressWarnings("fallthrough")
        public Matrix getMatrix(final ContextualParameters.MatrixRole role) throws FactoryException {
            final CoordinateSystem specified;
            boolean inverse = false;
            switch (role) {
                default: throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "role", role));
                case INVERSE_NORMALIZATION:   inverse   = true;          // Fall through
                case NORMALIZATION:           specified = getSourceCS(); break;
                case INVERSE_DENORMALIZATION: inverse   = true;          // Fall through
                case DENORMALIZATION:         inverse   = !inverse;
                                              specified = getTargetCS(); break;
            }
            if (specified == null) {
                return null;
            }
            final CoordinateSystem normalized = CoordinateSystems.replaceAxes(specified, AxesConvention.NORMALIZED);
            try {
                if (inverse) {
                    return CoordinateSystems.swapAndScaleAxes(normalized, specified);
                } else {
                    return CoordinateSystems.swapAndScaleAxes(specified, normalized);
                }
            } catch (IllegalArgumentException | IncommensurableException cause) {
                throw new InvalidGeodeticParameterException(cause.getLocalizedMessage(), cause);
            }
        }

        /**
         * Returns the operation method used for the math transform creation.
         * This is the same information than {@link #getLastMethodUsed()} but more stable
         * (not affected by transforms created with other contexts).
         *
         * @return the operation method used by the factory.
         * @throws IllegalStateException if {@link #createParameterizedTransform(ParameterValueGroup, Context)}
         *         has not yet been invoked.
         *
         * @see #getLastMethodUsed()
         *
         * @since 1.3
         */
        public OperationMethod getMethodUsed() {
            if (provider != null) {
                return provider;
            }
            throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedParameterValues));
        }

        /**
         * Returns the names of parameters that have been inferred from the context.
         * The set of keys can contain any of {@code "dim"},
         * {@code     "semi_major"}, {@code     "semi_minor"},
         * {@code "src_semi_major"}, {@code "src_semi_minor"},
         * {@code "tgt_semi_major"}, {@code "tgt_semi_minor"} and/or
         * {@code "inverse_flattening"}, depending on the operation method used.
         * The parameters named in that set are included in the parameters
         * returned by {@link #getCompletedParameters()}.
         *
         * <h4>Associated boolean values</h4>
         * The associated boolean in the map tells whether the named parameter value is really contextual.
         * The boolean is {@code FALSE} if the user explicitly specified a value in the parameters given to
         * the {@link #createParameterizedTransform(ParameterValueGroup, Context)} method,
         * and that value is different than the value inferred from the context.
         * Such inconsistencies are also logged at {@link Level#WARNING}.
         * In all other cases
         * (no value specified by the user, or a value was specified but is consistent with the context),
         * the associated boolean in the map is {@code TRUE}.
         *
         * <h4>Mutability</h4>
         * The returned map is modifiable for making easier for callers to amend the contextual information.
         * This map is not used by {@code Context} except for information purposes (e.g. in {@link #toString()}).
         * In particular, modifications of this map have no incidence on the created {@link MathTransform}.
         *
         * @return names of parameters inferred from context.
         *
         * @since 1.3
         */
        @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Modifiable by method contract.
        public Map<String,Boolean> getContextualParameters() {
            return contextualParameters;
        }

        /**
         * Returns the parameter values used for the math transform creation,
         * including the parameters completed by the factory.
         * The parameters inferred from the context are listed by {@link #getContextualParameters()}.
         *
         * @return the parameter values used by the factory.
         * @throws IllegalStateException if {@link #createParameterizedTransform(ParameterValueGroup, Context)}
         *         has not yet been invoked.
         */
        public ParameterValueGroup getCompletedParameters() {
            if (parameters != null) {
                return parameters;
            }
            throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedParameterValues));
        }

        /**
         * If the parameters given by the user were not created by {@code getDefaultParameters(String)}
         * or something equivalent, copies those parameters into the structure expected by the provider.
         * The intent is to make sure that we have room for the parameters that {@code setEllipsoids(…)}
         * may write.
         *
         * <p>A side effect of this method is that the copy operation may perform a check of
         * parameter value validity. This may result in an {@link InvalidParameterNameException}
         * or {@link InvalidParameterValueException} to be thrown.</p>
         *
         * @param  writable  {@code true} if this method should also check that the parameters group is editable.
         * @throws IllegalArgumentException if the copy cannot be performed because a parameter has
         *         a unrecognized name or an illegal value.
         */
        private void ensureCompatibleParameters(final boolean writable) throws IllegalArgumentException {
            final ParameterDescriptorGroup expected = provider.getParameters();
            if (parameters.getDescriptor() != expected || (writable && Parameters.isUnmodifiable(parameters))) {
                final ParameterValueGroup copy = expected.createValue();
                Parameters.copy(parameters, copy);
                parameters = copy;
            }
        }

        /**
         * Gets a parameter for which to infer a value from the context.
         * The consistency flag is initially set to {@link Boolean#TRUE}.
         *
         * @param  name  name of the contextual parameter.
         * @return the parameter.
         * @throws ParameterNotFoundException if the parameter was not found.
         */
        private ParameterValue<?> getContextualParameter(final String name) throws ParameterNotFoundException {
            ParameterValue<?> parameter = parameters.parameter(name);
            contextualParameters.put(name, Boolean.TRUE);               // Add only if above line succeeded.
            return parameter;
        }

        /**
         * Returns the value of the given parameter in the given unit, or {@code NaN} if the parameter is not set.
         *
         * <p><b>NOTE:</b> Do not merge this function with {@code ensureSet(…)}. We keep those two methods
         * separated in order to give to {@code createParameterizedTransform(…)} a "all or nothing" behavior.</p>
         */
        private static double getValue(final ParameterValue<?> parameter, final Unit<?> unit) {
            return (parameter.getValue() != null) ? parameter.doubleValue(unit) : Double.NaN;
        }

        /**
         * Ensures that a value is set in the given parameter.
         *
         * <ul>
         *   <li>If the parameter has no value, then it is set to the given value.<li>
         *   <li>If the parameter already has a value, then the parameter is left unchanged
         *       but its value is compared to the given one for consistency.</li>
         * </ul>
         *
         * @param  parameter  the parameter which must have a value.
         * @param  actual     the current parameter value, or {@code NaN} if none.
         * @param  expected   the expected parameter value, derived from the ellipsoid.
         * @param  unit       the unit of {@code value}.
         * @param  tolerance  maximal difference (in unit of {@code unit}) for considering the two values as equivalent.
         * @return {@code true} if there is a mismatch between the actual value and the expected one.
         */
        private static boolean ensureSet(final ParameterValue<?> parameter, final double actual,
                final double expected, final Unit<?> unit, final double tolerance)
        {
            if (Math.abs(actual - expected) <= tolerance) {
                return false;
            }
            if (Double.isNaN(actual)) {
                parameter.setValue(expected, unit);
                return false;
            }
            return true;
        }

        /**
         * Completes the parameter group with information about source or target ellipsoid axis lengths,
         * if available. This method writes semi-major and semi-minor parameter values only if they do not
         * already exists in the given parameters.
         *
         * @param  ellipsoid          the ellipsoid from which to get axis lengths of flattening factor, or {@code null}.
         * @param  semiMajor          {@code "semi_major}, {@code "src_semi_major} or {@code "tgt_semi_major} parameter name.
         * @param  semiMinor          {@code "semi_minor}, {@code "src_semi_minor} or {@code "tgt_semi_minor} parameter name.
         * @param  inverseFlattening  {@code true} if this method can try to set the {@code "inverse_flattening"} parameter.
         * @return the exception if the operation failed, or {@code null} if none. This exception is not thrown now
         *         because the caller may succeed in creating the transform anyway, or otherwise may produce a more
         *         informative exception.
         */
        private RuntimeException setEllipsoid(final Ellipsoid ellipsoid, final String semiMajor, final String semiMinor,
                final boolean inverseFlattening, RuntimeException failure)
        {
            /*
             * Note: we could also consider to set the "dim" parameter here based on the number of dimensions
             * of the coordinate system. But except for the Molodensky operation, this would be SIS-specific.
             * A more portable way is to concatenate a "Geographic 3D to 2D" operation after the transform if
             * we see that the dimensions do not match. It also avoid attempt to set a "dim" parameter on map
             * projections, which is not allowed.
             */
            if (ellipsoid != null) {
                ensureCompatibleParameters(true);
                ParameterValue<?> mismatchedParam = null;
                double mismatchedValue = 0;
                try {
                    final ParameterValue<?> ap = getContextualParameter(semiMajor);
                    final ParameterValue<?> bp = getContextualParameter(semiMinor);
                    final Unit<Length> unit = ellipsoid.getAxisUnit();
                    /*
                     * The two calls to getValue(…) shall succeed before we write anything, in order to have a
                     * "all or nothing" behavior as much as possible. Note that Ellipsoid.getSemi**Axis() have
                     * no reason to fail, so we do not take precaution for them.
                     */
                    final double a   = getValue(ap, unit);
                    final double b   = getValue(bp, unit);
                    final double tol = Units.METRE.getConverterTo(unit).convert(ELLIPSOID_PRECISION);
                    if (ensureSet(ap, a, ellipsoid.getSemiMajorAxis(), unit, tol)) {
                        contextualParameters.put(semiMajor, Boolean.FALSE);
                        mismatchedParam = ap;
                        mismatchedValue = a;
                    }
                    if (ensureSet(bp, b, ellipsoid.getSemiMinorAxis(), unit, tol)) {
                        contextualParameters.put(semiMinor, Boolean.FALSE);
                        mismatchedParam = bp;
                        mismatchedValue = b;
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    /*
                     * Parameter not found, or is not numeric, or unit of measurement is not linear.
                     * Do not touch to the parameters. We will see if createParameterizedTransform(…)
                     * can do something about that. If it cannot, createParameterizedTransform(…) is
                     * the right place to throw the exception.
                     */
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
                /*
                 * Following is specific to Apache SIS. We use this non-standard API for allowing the
                 * NormalizedProjection class (our base class for all map projection implementations)
                 * to known that the ellipsoid definitive parameter is the inverse flattening factor
                 * instead of the semi-major axis length. It makes a small difference in the accuracy
                 * of the eccentricity parameter.
                 */
                if (mismatchedParam == null && inverseFlattening && ellipsoid.isIvfDefinitive()) try {
                    final ParameterValue<?> ep = getContextualParameter(Constants.INVERSE_FLATTENING);
                    final double e = getValue(ep, Units.UNITY);
                    if (ensureSet(ep, e, ellipsoid.getInverseFlattening(), Units.UNITY, 1E-10)) {
                        contextualParameters.put(Constants.INVERSE_FLATTENING, Boolean.FALSE);
                        mismatchedParam = ep;
                        mismatchedValue = e;
                    }
                } catch (ParameterNotFoundException e) {
                    /*
                     * Should never happen with Apache SIS implementation, but may happen if the given parameters come
                     * from another implementation. We can safely abandon our attempt to set the inverse flattening value,
                     * since it was redundant with semi-minor axis length.
                     */
                    Logging.recoverableException(AbstractMathTransform.LOGGER,
                            DefaultMathTransformFactory.class, "createParameterizedTransform", e);
                }
                /*
                 * If a parameter was explicitly specified by user but has a value inconsistent with the context,
                 * log a warning. In addition, the associated boolean value in `contextualParameters` map should
                 * have been set to `Boolean.FALSE`.
                 */
                if (mismatchedParam != null) {
                    final LogRecord record = Resources.forLocale(null).getLogRecord(Level.WARNING,
                            Resources.Keys.MismatchedEllipsoidAxisLength_3, ellipsoid.getName().getCode(),
                            mismatchedParam.getDescriptor().getName().getCode(), mismatchedValue);
                    Logging.completeAndLog(AbstractMathTransform.LOGGER,
                            DefaultMathTransformFactory.class, "createParameterizedTransform", record);
                }
            }
            return failure;
        }

        /**
         * Completes the parameter group with information about source and target ellipsoid axis lengths,
         * if available. This method writes semi-major and semi-minor parameter values only if they do not
         * already exists in the given parameters.
         *
         * <p>The given method and parameters are stored in the {@link #provider} and {@link #parameters}
         * fields respectively. The actual stored values may differ from the values given to this method.</p>
         *
         * @param  factory  the enclosing factory.
         * @param  method   description of the transform to be created, or {@code null} if unknown.
         * @return the exception if the operation failed, or {@code null} if none. This exception is not thrown now
         *         because the caller may succeed in creating the transform anyway, or otherwise may produce a more
         *         informative exception.
         * @throws IllegalArgumentException if the operation fails because a parameter has a unrecognized name or an
         *         illegal value.
         *
         * @see #getCompletedParameters()
         */
        final RuntimeException completeParameters(final DefaultMathTransformFactory factory, OperationMethod method,
                final ParameterValueGroup userParams) throws FactoryException, IllegalArgumentException
        {
            /*
             * The "Geographic/geocentric conversions" conversion (EPSG:9602) can be either:
             *
             *    - "Ellipsoid_To_Geocentric"
             *    - "Geocentric_To_Ellipsoid"
             *
             * EPSG defines both by a single operation, but Apache SIS needs to distinguish them.
             */
            if (method instanceof AbstractProvider) {
                final String alt = ((AbstractProvider) method).resolveAmbiguity(this);
                if (alt != null) {
                    method = factory.getOperationMethod(alt);
                }
            }
            provider   = method;
            parameters = userParams;
            /*
             * Get the operation method for the appropriate number of dimensions. For example, the default Molodensky
             * operation expects two-dimensional source and target CRS. If a given CRS is three-dimensional, we need
             * a provider variant which will not concatenate a "geographic 3D to 2D" operation before the Molodensky
             * one.
             */
            if (method instanceof AbstractProvider) {
                final Integer sourceDim = (sourceCS != null) ? sourceCS.getDimension() : method.getSourceDimensions();
                final Integer targetDim = (targetCS != null) ? targetCS.getDimension() : method.getTargetDimensions();
                if (sourceDim != null && targetDim != null) {
                    method = ((AbstractProvider) method).redimension(sourceDim, targetDim);
                    if (method instanceof MathTransformProvider) {
                        provider = method;
                    }
                }
            }
            ensureCompatibleParameters(false);      // Invoke only after we set `provider` to its final instance.
            /*
             * Get a mask telling us if we need to set parameters for the source and/or target ellipsoid.
             * This information should preferably be given by the provider. But if the given provider is
             * not a SIS implementation, use as a fallback whether ellipsoids are provided. This fallback
             * may be less reliable.
             */
            final boolean sourceOnEllipsoid, targetOnEllipsoid;
            if (provider instanceof AbstractProvider) {
                final AbstractProvider p = (AbstractProvider) provider;
                sourceOnEllipsoid = p.sourceOnEllipsoid;
                targetOnEllipsoid = p.targetOnEllipsoid;
            } else {
                sourceOnEllipsoid = getSourceEllipsoid() != null;
                targetOnEllipsoid = getTargetEllipsoid() != null;
            }
            /*
             * Set the ellipsoid axis-length parameter values. Those parameters may appear in the source ellipsoid,
             * in the target ellipsoid or in both ellipsoids.
             */
            if (!(sourceOnEllipsoid | targetOnEllipsoid)) return null;
            if (!targetOnEllipsoid) return setEllipsoid(getSourceEllipsoid(), Constants.SEMI_MAJOR, Constants.SEMI_MINOR, true, null);
            if (!sourceOnEllipsoid) return setEllipsoid(getTargetEllipsoid(), Constants.SEMI_MAJOR, Constants.SEMI_MINOR, true, null);
            RuntimeException failure = null;
            if (sourceCS != null) try {
                ensureCompatibleParameters(true);
                final ParameterValue<?> p = getContextualParameter(Constants.DIM);
                if (p.getValue() == null) {
                    p.setValue(sourceCS.getDimension());
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                failure = e;
            }
            failure = setEllipsoid(getSourceEllipsoid(), "src_semi_major", "src_semi_minor", false, failure);
            failure = setEllipsoid(getTargetEllipsoid(), "tgt_semi_major", "tgt_semi_minor", false, failure);
            return failure;
        }

        /**
         * Returns a string representation of this context for debugging purposes.
         * Current implementation write the name of source/target coordinate systems and ellipsoids.
         * If {@linkplain #getContextualParameters() contextual parameters} have already been inferred,
         * then their names are appended with inconsistent parameters (if any) written on a separated line.
         *
         * @return a string representation of this context.
         */
        @Override
        public String toString() {
            final Object[] properties = {
                "sourceCS", sourceCS, "sourceEllipsoid", sourceEllipsoid,
                "targetCS", targetCS, "targetEllipsoid", targetEllipsoid
            };
            for (int i=1; i<properties.length; i += 2) {
                final IdentifiedObject value = (IdentifiedObject) properties[i];
                if (value != null) properties[i] = value.getName();
            }
            String text = Strings.toString(getClass(), properties);
            if (!contextualParameters.isEmpty()) {
                final StringBuilder b = new StringBuilder(text);
                boolean isContextual = true;
                do {
                    boolean first = true;
                    for (final Map.Entry<String,Boolean> entry : contextualParameters.entrySet()) {
                        if (entry.getValue() == isContextual) {
                            if (first) {
                                first = false;
                                b.append(System.lineSeparator())
                                 .append(isContextual ? "Contextual parameters" : "Inconsistencies").append(": ");
                            } else {
                                b.append(", ");
                            }
                            b.append(entry.getKey());
                        }
                    }
                } while ((isContextual = !isContextual) == false);
                text = b.toString();
            }
            return text;
        }
    }

    /**
     * Creates a transform from a group of parameters.
     * The set of expected parameters varies for each operation.
     * The easiest way to provide parameter values is to get an initially empty group for the desired
     * operation by calling {@link #getDefaultParameters(String)}, then to fill the parameter values.
     * Example:
     *
     * {@snippet lang="java" :
     *     ParameterValueGroup group = factory.getDefaultParameters("Transverse_Mercator");
     *     group.parameter("semi_major").setValue(6378137.000);
     *     group.parameter("semi_minor").setValue(6356752.314);
     *     MathTransform mt = factory.createParameterizedTransform(group, null);
     *     }
     *
     * Sometimes the {@code "semi_major"} and {@code "semi_minor"} parameter values are not explicitly provided,
     * but rather inferred from the {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic
     * datum} of the source Coordinate Reference System. If the given {@code context} argument is non-null,
     * then this method will use those contextual information for:
     *
     * <ol>
     *   <li>Inferring the {@code "semi_major"}, {@code "semi_minor"}, {@code "src_semi_major"},
     *       {@code "src_semi_minor"}, {@code "tgt_semi_major"} or {@code "tgt_semi_minor"} parameter values
     *       from the {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoids} associated to
     *       the source or target CRS, if those parameters are not explicitly given and if they are relevant
     *       for the coordinate operation method.</li>
     *   <li>{@linkplain #createConcatenatedTransform Concatenating} the parameterized transform
     *       with any other transforms required for performing units changes and coordinates swapping.</li>
     * </ol>
     *
     * The complete group of parameters, including {@code "semi_major"}, {@code "semi_minor"} or other calculated values,
     * can be obtained by a call to {@link Context#getCompletedParameters()} after {@code createParameterizedTransform(…)}
     * returned. Note that the completed parameters may only have additional parameters compared to the given parameter
     * group; existing parameter values should not be modified.
     *
     * <p>The {@code OperationMethod} instance used by this constructor can be obtained by a call to
     * {@link #getLastMethodUsed()}.</p>
     *
     * @param  parameters  the parameter values. The {@linkplain ParameterDescriptorGroup#getName() parameter group name}
     *                     shall be the name of the desired {@linkplain DefaultOperationMethod operation method}.
     * @param  context     information about the context (for example source and target coordinate systems)
     *                     in which the new transform is going to be used, or {@code null} if none.
     * @return the transform created from the given parameters.
     * @throws NoSuchIdentifierException if there is no method for the given parameter group name.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see #getDefaultParameters(String)
     * @see #getAvailableMethods(Class)
     * @see #getLastMethodUsed()
     * @see org.apache.sis.parameter.ParameterBuilder#createGroupForMapProjection(ParameterDescriptor...)
     */
    public MathTransform createParameterizedTransform(ParameterValueGroup parameters,
            final Context context) throws NoSuchIdentifierException, FactoryException
    {
        OperationMethod  method  = null;
        RuntimeException failure = null;
        MathTransform transform;
        try {
            ArgumentChecks.ensureNonNull("parameters", parameters);
            final ParameterDescriptorGroup descriptor = parameters.getDescriptor();
            final String methodName = descriptor.getName().getCode();
            String methodIdentifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(descriptor, Citations.EPSG));
            if (methodIdentifier == null) {
                methodIdentifier = methodName;
            }
            /*
             * Get the MathTransformProvider of the same name or identifier than the given parameter group.
             * We give precedence to EPSG identifier because operation method names are sometimes ambiguous
             * (e.g. "Lambert Azimuthal Equal Area (Spherical)"). If we fail to find the method by its EPSG code,
             * we will try searching by method name. As a side effect, this second attempt will produce a better
             * error message if the method is really not found.
             */
            try {
                method = getOperationMethod(methodIdentifier);
            } catch (NoSuchIdentifierException exception) {
                if (methodIdentifier.equals(methodName)) {
                    throw exception;
                }
                method = getOperationMethod(methodName);
                Logging.recoverableException(AbstractMathTransform.LOGGER,
                        DefaultMathTransformFactory.class, "createParameterizedTransform", exception);
            }
            if (!(method instanceof MathTransformProvider)) {
                throw new NoSuchIdentifierException(Errors.format(          // For now, handle like an unknown operation.
                        Errors.Keys.UnsupportedImplementation_1, Classes.getClass(method)), methodName);
            }
            /*
             * Will catch only exceptions that may be the result of improper parameter usage (e.g. a value out
             * of range). Do not catch exceptions caused by programming errors (e.g. null pointer exception).
             */
            try {
                /*
                 * If the user's parameters do not contain semi-major and semi-minor axis lengths, infer
                 * them from the ellipsoid. We have to do that because those parameters are often omitted,
                 * since the standard place where to provide this information is in the ellipsoid object.
                 */
                if (context != null) {
                    failure    = context.completeParameters(this, method, parameters);
                    parameters = context.parameters;
                    method     = context.provider;
                }
                transform = ((MathTransformProvider) method).createMathTransform(this, parameters);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
            }
            /*
             * Cache the transform that we just created and make sure that the number of dimensions
             * is compatible with the OperationMethod instance. Then make final adjustment for axis
             * directions and units of measurement.
             */
            transform = unique(transform);
            if (method instanceof AbstractProvider) {
                method = ((AbstractProvider) method).variantFor(transform);
            }
            if (context != null) {
                transform = swapAndScaleAxes(transform, context);
            }
        } catch (FactoryException e) {
            if (failure != null) {
                e.addSuppressed(failure);
            }
            throw e;
        } finally {
            lastMethod.set(method);     // May be null in case of failure, which is intended.
        }
        return transform;
    }

    /**
     * Given a transform between normalized spaces,
     * creates a transform taking in account axis directions, units of measurement and longitude rotation.
     * This method {@linkplain #createConcatenatedTransform concatenates} the given parameterized transform
     * with any other transform required for performing units changes and coordinates swapping.
     *
     * <p>The given {@code parameterized} transform shall expect
     * {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED normalized} input coordinates and
     * produce normalized output coordinates. See {@link org.apache.sis.referencing.cs.AxesConvention} for more
     * information about what Apache SIS means by "normalized".</p>
     *
     * <h4>Example</h4>
     * The most typical examples of transforms with normalized inputs/outputs are normalized
     * map projections expecting (<cite>longitude</cite>, <cite>latitude</cite>) inputs in degrees
     * and calculating (<cite>x</cite>, <cite>y</cite>) coordinates in metres,
     * both of them with ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
     * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) axis orientations.
     *
     * <h4>Controlling the normalization process</h4>
     * Users who need a different normalized space than the default one way find more convenient to
     * override the {@link Context#getMatrix Context.getMatrix(ContextualParameters.MatrixRole)} method.
     *
     * @param  parameterized  a transform for normalized input and output coordinates.
     * @param  context        source and target coordinate systems in which the transform is going to be used.
     * @return a transform taking in account unit conversions and axis swapping.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.AxesConvention#NORMALIZED
     * @see org.apache.sis.referencing.operation.DefaultConversion#DefaultConversion(Map, OperationMethod, MathTransform, ParameterValueGroup)
     *
     * @since 0.7
     */
    public MathTransform swapAndScaleAxes(final MathTransform parameterized, final Context context) throws FactoryException {
        ArgumentChecks.ensureNonNull("parameterized", parameterized);
        ArgumentChecks.ensureNonNull("context", context);
        /*
         * Compute matrices for swapping axis and performing units conversion.
         * There is one matrix to apply before projection from (λ,φ) coordinates,
         * and one matrix to apply after projection on (easting,northing) coordinates.
         */
        final Matrix swap1 = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final Matrix swap3 = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        /*
         * Prepare the concatenation of the matrices computed above and the projection.
         * Note that at this stage, the dimensions between each step may not be compatible.
         * For example, the projection (step2) is usually two-dimensional while the source
         * coordinate system (step1) may be three-dimensional if it has a height.
         */
        MathTransform step1 = (swap1 != null) ? createAffineTransform(swap1) : MathTransforms.identity(parameterized.getSourceDimensions());
        MathTransform step3 = (swap3 != null) ? createAffineTransform(swap3) : MathTransforms.identity(parameterized.getTargetDimensions());
        MathTransform step2 = parameterized;
        /*
         * Special case for the way EPSG handles reversal of axis direction. For now the "Vertical Offset" (EPSG:9616)
         * method is the only one for which we found a need for special case. But if more special cases are added in a
         * future SIS version, then we should replace the static method by a non-static one defined in AbstractProvider.
         */
        if (context.provider instanceof VerticalOffset) {
            step2 = VerticalOffset.postCreate(step2, swap3);
        }
        /*
         * If the target coordinate system has a height, instruct the projection to pass the height unchanged from
         * the base CRS to the target CRS. After this block, the dimensions of `step2` and `step3` should match.
         *
         * The height is always the last dimension in a normalized EllipdoidalCS. We accept only a hard-coded list
         * of dimensions because it is not `MathTransformFactory` job to build a transform chain in a generic way.
         * We handle only the cases that are necessary because of the way some operation methods are provided.
         * In particular Apache SIS provides only 2D map projections, so 3D projections have to be "generated"
         * on the fly. That use case is:
         *
         *     - Source CRS: a GeographicCRS (regardless its number of dimension – it will be addressed in next block)
         *     - Target CRS: a 3D ProjectedCRS
         *     - Parameterized transform: a 2D map projection. We need the ellipsoidal height to passthrough.
         *
         * The reverse order (projected source CRS and geographic target CRS) is also accepted but should be uncommon.
         */
        final int resultDim = step3.getSourceDimensions();              // Final result (minus trivial changes).
        final int kernelDim = step2.getTargetDimensions();              // Result of the core part of transform.
        final int numTrailingCoordinates = resultDim - kernelDim;
        if (numTrailingCoordinates != 0) {
            ensureDimensionChangeAllowed(parameterized, context, numTrailingCoordinates, resultDim);
            if (numTrailingCoordinates > 0) {
                step2 = createPassThroughTransform(0, step2, numTrailingCoordinates);
            } else {
                step2 = createConcatenatedTransform(step2, createAffineTransform(
                        Matrices.createDimensionSelect(kernelDim, ArraysExt.range(0, resultDim))));
            }
        }
        /*
         * If the source CS has a height but the target CS doesn't, drops the extra coordinates.
         * Conversely if the source CS is missing a height, add a height with NaN values.
         * After this block, the dimensions of `step1` and `step2` should match.
         *
         * When adding an ellipsoidal height, there is two scenarios: the ellipsoidal height may be used by the
         * parameterized operation, or it may be passed through (in which case the operation ignores the height).
         * If the height is expected as operation input, set the height to 0. Otherwise (the pass through case),
         * set the height to NaN. We do that way because the given `parameterized` transform may be a Molodensky
         * transform or anything else that could use the height in its calculation. If we have to add a height as
         * a pass through dimension, maybe the parameterized transform is a 2D Molodensky instead of a 3D Molodensky.
         * The result of passing through the height is not the same as if a 3D Molodensky was used in the first place.
         * A NaN value avoid to give a false sense of accuracy.
         */
        final int sourceDim = step1.getTargetDimensions();
        final int targetDim = step2.getSourceDimensions();
        int insertCount = targetDim - sourceDim;
        if (insertCount != 0) {
            ensureDimensionChangeAllowed(parameterized, context, insertCount, targetDim);
            final Matrix resize = Matrices.createZero(targetDim+1, sourceDim+1);
            for (int j=0; j<targetDim; j++) {
                resize.setElement(j, Math.min(j, sourceDim), (j < sourceDim) ? 1 :
                        ((--insertCount >= numTrailingCoordinates) ? 0 : Double.NaN));        // See above note.
            }
            resize.setElement(targetDim, sourceDim, 1);     // Element in the lower-right corner.
            step1 = createConcatenatedTransform(step1, createAffineTransform(resize));
        }
        MathTransform mt = createConcatenatedTransform(createConcatenatedTransform(step1, step2), step3);
        /*
         * At this point we finished to create the transform.  But before to return it, verify if the
         * parameterized transform given in argument had some custom parameters. This happen with the
         * Equirectangular projection, which can be simplified as an AffineTransform while we want to
         * continue to describe it with the "semi_major", "semi_minor", etc. parameters  instead of
         * "elt_0_0", "elt_0_1", etc.  The following code just forwards those parameters to the newly
         * created transform; it does not change the operation.
         */
        if (parameterized instanceof ParameterizedAffine && !(mt instanceof ParameterizedAffine)) {
            mt = ((ParameterizedAffine) parameterized).newTransform(mt);
        }
        return mt;
    }

    /**
     * Checks whether {@link #swapAndScaleAxes(MathTransform, Context)} should accept to adjust the number of
     * transform dimensions. Current implementation accepts only addition or removal of ellipsoidal height,
     * but future version may expand the list of accepted cases. The intent for this method is to catch errors
     * caused by wrong coordinate systems associated to a parameterized transform, keeping in mind that it is
     * not {@link DefaultMathTransformFactory} job to handle changes between arbitrary CRS (those changes are
     * handled by {@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory} instead).
     *
     * <h4>Implementation note</h4>
     * The {@code parameterized} transform is a black box receiving inputs in
     * any CS and producing outputs in any CS, not necessarily of the same kind. For that reason, we cannot use
     * {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} between the normalized CS.
     * We have to trust that the caller know that the coordinate systems (s)he provided are correct for the work
     * done by the transform.
     *
     * @param  parameterized  the parameterized transform, for producing an error message if needed.
     * @param  context        the source and target coordinate system.
     * @param  change         number of dimensions to add (if positive) or remove (if negative).
     * @param  resultDim      number of dimensions after the change.
     */
    private static void ensureDimensionChangeAllowed(final MathTransform parameterized,
            final Context context, final int change, final int resultDim) throws FactoryException
    {
        if (Math.abs(change) == 1 && resultDim >= 2 && resultDim <= 3) {
            if (context.getSourceCS() instanceof EllipsoidalCS ||
                context.getTargetCS() instanceof EllipsoidalCS)
            {
                return;
            }
        }
        /*
         * Creates the error message for a transform that cannot be associated with given coordinate systems.
         */
        String name = null;
        if (parameterized instanceof Parameterized) {
            name = IdentifiedObjects.getDisplayName(((Parameterized) parameterized).getParameterDescriptors(), null);
        }
        if (name == null) {
            name = Classes.getShortClassName(parameterized);
        }
        final StringBuilder b = new StringBuilder();
        CoordinateSystem cs = context.getSourceCS();
        if (cs != null) b.append(cs.getDimension()).append("D → ");
        b.append("tr(").append(parameterized.getSourceDimensions()).append("D → ")
                     .append(parameterized.getTargetDimensions()).append("D)");
        cs = context.getTargetCS();
        if (cs != null) b.append(" → ").append(cs.getDimension()).append('D');
        throw new InvalidGeodeticParameterException(Resources.format(Resources.Keys.CanNotAssociateToCS_2, name, b));
    }

    /**
     * Creates a transform from a base CRS to a derived CS using the given parameters.
     * If this method needs to set the values of {@code "semi_major"} and {@code "semi_minor"} parameters,
     * then it sets those values directly on the given {@code parameters} instance – not on a clone – for
     * allowing the caller to get back the complete parameter values.
     * However, this method only fills missing values, it never modify existing values.
     *
     * @param  baseCRS     the source coordinate reference system.
     * @param  parameters  the parameter values for the transform.
     * @param  derivedCS   the target coordinate system.
     * @return the parameterized transform from {@code baseCRS} to {@code derivedCS},
     *         including unit conversions and axis swapping.
     * @throws NoSuchIdentifierException if there is no transform registered for the coordinate operation method.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @deprecated Replaced by {@link #createParameterizedTransform(ParameterValueGroup, Context)}.
     */
    @Override
    @Deprecated(since="0.7")
    public MathTransform createBaseToDerived(final CoordinateReferenceSystem baseCRS,
            final ParameterValueGroup parameters, final CoordinateSystem derivedCS)
            throws NoSuchIdentifierException, FactoryException
    {
        ArgumentChecks.ensureNonNull("baseCRS",    baseCRS);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        ArgumentChecks.ensureNonNull("derivedCS",  derivedCS);
        final Context context = ReferencingUtilities.createTransformContext(baseCRS, null);
        context.setTarget(derivedCS);
        return createParameterizedTransform(parameters, context);
    }

    /**
     * Creates a math transform that represent a change of coordinate system. If exactly one argument is
     * an {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal coordinate systems},
     * then the {@code ellipsoid} argument is mandatory. In all other cases (including the case where both
     * coordinate systems are ellipsoidal), the ellipsoid argument is ignored and can be {@code null}.
     *
     * <h4>Design note</h4>
     * This method does not accept separated ellipsoid arguments for {@code source} and {@code target} because
     * this method should not be used for datum shifts. If the two given coordinate systems are ellipsoidal,
     * then they are assumed to use the same ellipsoid. If different ellipsoids are desired, then a
     * {@linkplain #createParameterizedTransform parameterized transform} like <cite>"Molodensky"</cite>,
     * <cite>"Geocentric translations"</cite>, <cite>"Coordinate Frame Rotation"</cite> or
     * <cite>"Position Vector transformation"</cite> should be used instead.
     *
     * @param  source     the source coordinate system.
     * @param  target     the target coordinate system.
     * @param  ellipsoid  the ellipsoid of {@code EllipsoidalCS}, or {@code null} if none.
     * @return a conversion from the given source to the given target coordinate system.
     * @throws FactoryException if the conversion cannot be created.
     *
     * @since 0.8
     */
    public MathTransform createCoordinateSystemChange(final CoordinateSystem source, final CoordinateSystem target,
            final Ellipsoid ellipsoid) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        lastMethod.remove();                                // In case an exception is thrown before completion.
        if (ellipsoid != null) {
            final boolean isEllipsoidalSource = (source instanceof EllipsoidalCS);
            if (isEllipsoidalSource != (target instanceof EllipsoidalCS)) {
                /*
                 * For now we support only conversion between EllipsoidalCS and CartesianCS.
                 * But future Apache SIS versions could add support for conversions between
                 * EllipsoidalCS and SphericalCS or other coordinate systems.
                 */
                if ((isEllipsoidalSource ? target : source) instanceof CartesianCS) {
                    final Context context = new Context();
                    final EllipsoidalCS cs;
                    final String operation;
                    if (isEllipsoidalSource) {
                        operation = GeographicToGeocentric.NAME;
                        context.setSource(cs = (EllipsoidalCS) source);
                        context.setTarget(target);
                        context.sourceEllipsoid = ellipsoid;
                    } else {
                        operation = GeocentricToGeographic.NAME;
                        context.setSource(source);
                        context.setTarget(cs = (EllipsoidalCS) target);
                        context.targetEllipsoid = ellipsoid;
                    }
                    final ParameterValueGroup pg = getDefaultParameters(operation);
                    if (cs.getDimension() < 3) {
                        pg.parameter(Constants.DIM).setValue(2);        // Apache SIS specific parameter.
                    }
                    return createParameterizedTransform(pg, context);
                }
            }
        }
        return CoordinateSystemTransform.create(this, source, target, lastMethod);
        // No need to use unique(…) here.
    }

    /**
     * Creates an affine transform from a matrix. If the transform input dimension is {@code M},
     * and output dimension is {@code N}, then the matrix will have size {@code [N+1][M+1]}. The
     * +1 in the matrix dimensions allows the matrix to do a shift, as well as a rotation. The
     * {@code [M][j]} element of the matrix will be the j'th coordinate of the moved origin. The
     * {@code [i][N]} element of the matrix will be 0 for <var>i</var> less than {@code M}, and 1
     * for <var>i</var> equals {@code M}.
     *
     * @param  matrix  the matrix used to define the affine transform.
     * @return the affine transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#linear(Matrix)
     */
    @Override
    public MathTransform createAffineTransform(final Matrix matrix) throws FactoryException {
        /*
         * Performance note: we could set lastMethod to the "Affine" operation method provider, but we do not
         * because setting this value is not free (e.g. it depends on matrix size) and it is rarely needed.
         */
        lastMethod.remove();
        return unique(MathTransforms.linear(matrix));
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two transforms, one after the other.
     *
     * <p>The dimension of the output space of the first transform must match the dimension of the input space
     * in the second transform. In order to concatenate more than two transforms, use this constructor repeatedly.</p>
     *
     * @param  tr1  the first transform to apply to points.
     * @param  tr2  the second transform to apply to points.
     * @return the concatenated transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#concatenate(MathTransform, MathTransform)
     */
    @Override
    public MathTransform createConcatenatedTransform(final MathTransform tr1,
                                                     final MathTransform tr2)
            throws FactoryException
    {
        lastMethod.remove();
        ArgumentChecks.ensureNonNull("tr1", tr1);
        ArgumentChecks.ensureNonNull("tr2", tr2);
        final MathTransform tr;
        try {
            tr = ConcatenatedTransform.create(tr1, tr2, this);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        assert MathTransforms.isValid(MathTransforms.getSteps(tr)) : tr;
        return unique(tr);
    }

    /**
     * Creates a transform which passes through a subset of coordinates to another transform.
     * This allows transforms to operate on a subset of coordinates.
     * The resulting transform will have the following dimensions:
     *
     * {@snippet lang="java" :
     *     int sourceDim = firstAffectedCoordinate + subTransform.getSourceDimensions() + numTrailingCoordinates;
     *     int targetDim = firstAffectedCoordinate + subTransform.getTargetDimensions() + numTrailingCoordinates;
     *     }
     *
     * <h4>Example</h4>
     * Giving (<var>latitude</var>, <var>longitude</var>, <var>height</var>) coordinates,
     * a pass through transform can convert the height values from meters to feet without
     * affecting the (<var>latitude</var>, <var>longitude</var>) values.
     *
     * @param  firstAffectedCoordinate  the lowest index of the affected coordinates.
     * @param  subTransform             transform to use for affected coordinates.
     * @param  numTrailingCoordinates   number of trailing coordinates to pass through. Affected coordinates will range
     *         from {@code firstAffectedCoordinate} inclusive to {@code dimTarget-numTrailingCoordinates} exclusive.
     * @return a pass through transform.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createPassThroughTransform(final int firstAffectedCoordinate,
                                                    final MathTransform subTransform,
                                                    final int numTrailingCoordinates)
            throws FactoryException
    {
        lastMethod.remove();
        final MathTransform tr;
        try {
            tr = MathTransforms.passThrough(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return unique(tr);
    }

    /**
     * There is no XML format for math transforms.
     *
     * @param  xml  math transform encoded in XML format.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @Deprecated
    public MathTransform createFromXML(String xml) throws FactoryException {
        lastMethod.remove();
        throw new FactoryException(Errors.format(Errors.Keys.UnsupportedOperation_1, "createFromXML"));
    }

    /**
     * Creates a math transform object from a
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
     * Known Text</cite> (WKT)</a>.
     * If the given text contains non-fatal anomalies (unknown or unsupported WKT elements,
     * inconsistent unit definitions, <i>etc.</i>), warnings may be reported in a
     * {@linkplain java.util.logging.Logger logger} named {@code "org.apache.sis.io.wkt"}.
     *
     * <p>Note that the WKT format is not always lossless. A {@code MathTransform} recreated from WKT may be
     * non-invertible even if the original transform was invertible. For example if an "Affine" operation is
     * defined by a non-square matrix, Apache SIS implementation sometimes has "hidden" information about the
     * inverse matrix but those information are lost at WKT formatting time. A similar "hidden" information
     * lost may also happen with {@link WraparoundTransform}, also making that transform non-invertible.</p>
     *
     * @param  text  math transform encoded in Well-Known Text format.
     * @return the math transform (never {@code null}).
     * @throws FactoryException if the Well-Known Text cannot be parsed,
     *         or if the math transform creation failed from some other reason.
     */
    @Override
    public MathTransform createFromWKT(final String text) throws FactoryException {
        lastMethod.remove();
        ArgumentChecks.ensureNonEmpty("text", text);
        Parser p = parser.getAndSet(null);
        if (p == null) try {
            Constructor<? extends Parser> c = parserConstructor;
            if (c == null) {
                c = Class.forName("org.apache.sis.io.wkt.MathTransformParser").asSubclass(Parser.class)
                         .getConstructor(MathTransformFactory.class);
                c.setAccessible(true);
                parserConstructor = c;
            }
            p = c.newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new FactoryException(e);
        }
        /*
         * No need to check the type of the parsed object, because MathTransformParser
         * should return only instance of MathTransform.
         */
        final Object object;
        try {
            object = p.createFromWKT(text);
        } catch (FactoryException e) {
            /*
             * The parsing may fail because a operation parameter is not known to SIS. If this happen, replace
             * the generic exception thrown be the parser (which is FactoryException) by a more specific one.
             * Note that InvalidGeodeticParameterException is defined only in this sis-referencing module,
             * so we could not throw it from the sis-metadata module that contain the parser.
             */
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ParameterNotFoundException) {
                    throw new InvalidGeodeticParameterException(e.getLocalizedMessage(), cause);
                }
                cause = cause.getCause();
            }
            throw e;
        }
        parser.set(p);
        return (MathTransform) object;
    }

    /**
     * Replaces the given transform by a unique instance, if one already exists.
     */
    private MathTransform unique(final MathTransform tr) {
        return (pool != null) ? pool.unique(tr) : tr;
    }

    /**
     * Returns the operation method used by the latest call to a {@code create(…)} constructor
     * in the currently running thread. Returns {@code null} if not applicable.
     *
     * <p>Invoking {@code getLastMethodUsed()} can be useful after a call to
     * {@link #createParameterizedTransform createParameterizedTransform(…)}.</p>
     *
     * @return the last method used by a {@code create(…)} constructor, or {@code null} if unknown of unsupported.
     *
     * @see #createParameterizedTransform(ParameterValueGroup, Context)
     * @see Context#getMethodUsed()
     */
    @Override
    public OperationMethod getLastMethodUsed() {
        return lastMethod.get();
    }

    /**
     * Notifies this factory that the elements provided by the {@code Iterable<OperationMethod>} may have changed.
     * This method performs the following steps:
     *
     * <ul>
     *   <li>Clears all caches.</li>
     *   <li>If the {@code Iterable} given at construction time is an instance of {@link ServiceLoader},
     *       invokes its {@code reload()} method.</li>
     * </ul>
     *
     * This method is useful to sophisticated applications which dynamically make new plug-ins available at runtime,
     * for example following changes of the application classpath.
     *
     * @see #DefaultMathTransformFactory(Iterable)
     * @see ServiceLoader#reload()
     */
    public void reload() {
        synchronized (methods) {
            methodsByName.clear();
            final Iterable<? extends OperationMethod> m = methods;
            if (m instanceof ServiceLoader<?>) {
                ((ServiceLoader<?>) m).reload();
            }
            synchronized (methodsByType) {
                for (final OperationMethodSet c : methodsByType.values()) {
                    c.reset();
                }
            }
            if (pool != null) {
                pool.clear();
            }
        }
    }
}
