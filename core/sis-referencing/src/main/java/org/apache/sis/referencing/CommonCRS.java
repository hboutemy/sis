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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.time.Instant;
import javax.measure.Unit;
import javax.measure.quantity.Time;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.referencing.datum.DefaultTemporalDatum;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultTimeCS;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Utilities;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.internal.util.StandardDateFormat.MILLISECONDS_PER_DAY;


/**
 * Frequently-used geodetic CRS and datum that are guaranteed to be available in SIS.
 * Some (not all) objects defined in this enumeration are equivalent to objects defined
 * in the EPSG geodetic dataset. In such case there is a choice:
 *
 * <ul class="verbose">
 *   <li>If the <a href="https://sis.apache.org/epsg.html">EPSG dataset is installed</a>, then the methods
 *       in this enumeration are effectively shortcuts for object definitions in the EPSG database.</li>
 *   <li>If there is no EPSG database available, or if the query failed, or if there is no EPSG definition
 *       for an object, then {@code CommonCRS} fallbacks on hard-coded values with minimal information.
 *       The {@linkplain AbstractIdentifiedObject#getIdentifiers() identifier} associated to the returned
 *       object should be interpreted as "see that EPSG code for more complete definition".</li>
 * </ul>
 *
 * Consequently, the methods in this enumeration never return {@code null}.
 * The definitions used as fallbacks are available in public sources
 * and do not include EPSG metadata except the identifier.
 * If the EPSG geodetic dataset has been used, the {@linkplain NamedIdentifier#getAuthority() authority} title
 * will be something like <cite>"EPSG geodetic dataset"</cite>, otherwise it will be <cite>"Subset of EPSG"</cite>.
 *
 * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code CommonCRS}
 * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
 * (e.g. the application is running in a container environment and some modules have been installed or uninstalled).</p>
 *
 * <h2>Example</h2>
 * The following code fetches a geographic Coordinate Reference System
 * using (<var>longitude</var>, <var>latitude</var>) axis order on the {@link #WGS84} geodetic datum:
 *
 * {@snippet lang="java" :
 *     GeographicCRS crs = CommonCRS.WGS84.normalizedGeographic();
 *     }
 *
 * <h2>Available objects</h2>
 * For each enumeration value, the name of the CRS, datum and ellipsoid objects may or may not be the same.
 * Below is an alphabetical list of object names available in this enumeration:
 *
 * <blockquote><table class="sis">
 *   <caption>Geodetic objects accessible by enumeration constants</caption>
 *   <tr><th>Name or alias</th>                                     <th>Object type</th>           <th>Enumeration value</th></tr>
 *   <tr><td>Clarke 1866</td>                                       <td>Ellipsoid</td>             <td>{@link #NAD27}</td></tr>
 *   <tr><td>European Datum 1950 (ED50)</td>                        <td>CRS, datum</td>            <td>{@link #ED50}</td></tr>
 *   <tr><td>European Terrestrial Reference System (ETRS) 1989</td> <td>CRS, datum</td>            <td>{@link #ETRS89}</td></tr>
 *   <tr><td>Greenwich</td>                                         <td>Prime meridian</td>        <td>Any enumeration value</td></tr>
 *   <tr><td>GRS 1980</td>                                          <td>Ellipsoid</td>             <td>{@link #GRS1980}, {@link #ETRS89}, {@link #NAD83}</td></tr>
 *   <tr><td>GRS 1980 Authalic Sphere</td>                          <td>Ellipsoid</td>             <td>{@link #SPHERE}</td></tr>
 *   <tr><td>International 1924</td>                                <td>Ellipsoid</td>             <td>{@link #ED50}</td></tr>
 *   <tr><td>North American Datum 1927</td>                         <td>CRS, datum</td>            <td>{@link #NAD27}</td></tr>
 *   <tr><td>North American Datum 1983</td>                         <td>CRS, datum</td>            <td>{@link #NAD83}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1972</td>                  <td>CRS, datum, ellipsoid</td> <td>{@link #WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1984</td>                  <td>CRS, datum, ellipsoid</td> <td>{@link #WGS84}</td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see org.apache.sis.referencing.factory.CommonAuthorityFactory
 *
 * @since 0.4
 */
@SuppressWarnings("DoubleCheckedLocking")
public enum CommonCRS {
    /**
     * World Geodetic System 1984.
     * This is the default CRS for most {@code org.apache.sis} packages.
     *
     * <blockquote><table class="compact">
     * <caption>WGS84 properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td>CRS:84, EPSG:4326</td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>World Geodetic System 1984 (WGS 84)</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 metres <i>(approximated)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257223563 <i>(definitive)</i></td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 60 in North and South hemispheres</td></tr>
     * </table></blockquote>
     */
    WGS84((short) 4326, (short) 4979, (short) 4978, (short) 6326, (short) 7030,             // Geodetic info
          (short) 5041, (short) 5042, (short) 32600, (short) 32700, (byte) 1, (byte) 60),   // UPS and UTM info

    /**
     * World Geodetic System 1972.
     *
     * <blockquote><table class="compact">
     * <caption>WGS72 properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td>EPSG:4322</td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>World Geodetic System 1972 (WGS 72)</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378135 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356751 metres <i>(approximated)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.26 <i>(definitive)</i></td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 60 in North and South hemispheres</td></tr>
     * </table></blockquote>
     */
    WGS72((short) 4322, (short) 4985, (short) 4984, (short) 6322, (short) 7043,             // Geodetic info
          (short) 0, (short) 0, (short) 32200, (short) 32300, (byte) 1, (byte) 60),         // UPS and UTM info

    /**
     * North American Datum 1983.
     * The ellipsoid is <cite>"GRS 1980"</cite>.
     * This ellipsoid is very close, but not identical, to the {@linkplain #WGS84} one.
     *
     * <blockquote><table class="compact">
     * <caption>NAD83 properties</caption>
     *   <tr><th>CRS identifier:</th>          <td>CRS:83, EPSG:4269</td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>North American Datum 1983 (NAD83)</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 metres <i>(approximated)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 23 in the North hemisphere</td></tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Note:</b>
     * {@link #ETRS89} uses the same ellipsoid for a different datum.
     * The <cite>Web Map Server</cite> {@code "CRS:83"} authority code uses the NAD83 datum,
     * while the {@code "IGNF:MILLER"} authority code uses the GRS80 datum.</div>
     */
    NAD83((short) 4269, (short) 0, (short) 0, (short) 6269, (short) 7019,                   // Geodetic info
          (short) 0, (short) 0, (short) 26900, (short) 0, (byte) 1, (byte) 23),             // UPS and UTM info

    /**
     * North American Datum 1927.
     *
     * <blockquote><table class="compact">
     * <caption>NAD27 properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td>CRS:27, EPSG:4267</td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>North American Datum 1927 (NAD27)</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378206.4 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356583.8 metres <i>(definitive)</i></td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 22 in the North hemisphere</td></tr>
     * </table></blockquote>
     */
    NAD27((short) 4267, (short) 0, (short) 0, (short) 6267, (short) 7008,                   // Geodetic info
          (short) 0, (short) 0, (short) 26700, (short) 0, (byte) 1, (byte) 22),             // UPS and UTM info

    /**
     * European Terrestrial Reference System 1989.
     * The ellipsoid is <cite>"GRS 1980"</cite>.
     * This ellipsoid is very close, but not identical, to the {@linkplain #WGS84} one.
     *
     * <blockquote><table class="compact">
     * <caption>ETRS89 properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td>EPSG:4258</td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>European Terrestrial Reference System 1989 (ETRS89)</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 metres <i>(approximated)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     *   <tr><th>UTM zones:</th>               <td>28 to 37 in the North hemisphere</td></tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Note:</b>
     * {@link #NAD83} uses the same ellipsoid for a different datum.
     * The <cite>Web Map Server</cite> {@code "CRS:83"} authority code uses the NAD83 datum,
     * while the {@code "IGNF:MILLER"} authority code uses the GRS80 datum.</div>
     */
    ETRS89((short) 4258, (short) 4937, (short) 4936, (short) 6258, (short) 7019,            // Geodetic info
           (short) 0, (short) 0, (short) 25800, (short) 0, (byte) 28, (byte) 37),           // UPS and UTM info

    /**
     * European Datum 1950 (ED50).
     *
     * <blockquote><table class="compact">
     * <caption>ED50 properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td>EPSG:4230</td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>European Datum 1950 (ED50)</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378388 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356912 metres <i>(approximated)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>297 <i>(definitive)</i></td></tr>
     *   <tr><th>UTM zones:</th>               <td>28 to 38 in the North hemisphere</td></tr>
     * </table></blockquote>
     */
    ED50((short) 4230, (short) 0, (short) 0, (short) 6230, (short) 7022,                    // Geodetic info
         (short) 0, (short) 0, (short) 23000, (short) 0, (byte) 28, (byte) 38),             // UPS and UTM info

    /**
     * Unknown datum based upon the GRS 1980 ellipsoid.
     * Use only in cases where geodetic datum is unknown.
     *
     * <blockquote><table class="compact">
     * <caption>GRS1980 properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td><del>EPSG:4019</del></td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>Unknown datum based upon the GRS 1980 ellipsoid</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 metres <i>(approximated)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     * </table></blockquote>
     *
     * @since 1.0
     */
    GRS1980((short) 4019, (short) 0, (short) 0, (short) 6019, (short) 7019,                  // Geodetic info
            (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0),                 // UPS and UTM info

    /**
     * Unspecified datum based upon the GRS 1980 Authalic Sphere.
     *
     * <blockquote><table class="compact">
     * <caption>Sphere properties</caption>
     *   <tr><th>CRS identifiers:</th>         <td><del>EPSG:4047</del></td></tr>
     *   <tr><th>Name and abbreviation:</th>   <td>Unspecified datum based upon the GRS 1980 Authalic Sphere</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6371007 metres</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6371007 metres <i>(definitive)</i></td></tr>
     * </table></blockquote>
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()
     */
    SPHERE((short) 4047, (short) 0, (short) 0, (short) 6047, (short) 7048,                  // Geodetic info
           (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0);                 // UPS and UTM info

    /**
     * The enum for the default CRS.
     *
     * @see #defaultGeographic()
     */
    static final CommonCRS DEFAULT = WGS84;

    /**
     * The EPSG code of the two-dimensional geographic CRS.
     */
    final short geographic;

    /**
     * The EPSG code of the three-dimensional geographic CRS, or 0 if none.
     * For non-zero value, this is often the {@link #geocentric} code + 1.
     */
    final short geo3D;

    /**
     * The EPSG code of the geocentric CRS, or 0 if none.
     */
    final short geocentric;

    /**
     * The EPSG code of the datum. The value is often {@link #geographic} + 2000,
     * but it doesn't have to be always the case.
     */
    final short datum;

    /**
     * The EPSG code of the ellipsoid.
     */
    final short ellipsoid;

    /**
     * EPSG codes of Universal Polar Stereographic projections, North and South cases.
     */
    final short northUPS, southUPS;

    /**
     * EPSG codes of pseudo "UTM zone zero" (North case and South case), or 0 if none.
     */
    final short northUTM, southUTM;

    /**
     * Zone number of the first UTM and last UTM zone defined in the EPSG database, inclusive.
     */
    final byte firstZone, lastZone;

    /**
     * The cached object. This is initially {@code null}, then set to various kinds of objects depending
     * on which method has been invoked. The kind of object stored in this field may change during the
     * application execution.
     */
    private transient volatile IdentifiedObject cached;

    /**
     * The normalized geographic CRS, created when first needed.
     *
     * @see #normalizedGeographic()
     */
    private transient volatile GeographicCRS cachedNormalized;

    /**
     * The three-dimensional geographic CRS, created when first needed.
     *
     * @see #geographic3D()
     */
    private transient volatile GeographicCRS cachedGeo3D;

    /**
     * The geocentric CRS using Cartesian coordinate system, created when first needed.
     *
     * @see #geocentric()
     */
    private transient volatile GeocentricCRS cachedGeocentric;

    /**
     * The geocentric CRS using spherical coordinate system, created when first needed.
     *
     * @see #spherical()
     */
    private transient volatile GeocentricCRS cachedSpherical;

    /**
     * The Universal Transverse Mercator (UTM) or Universal Polar Stereographic (UPS) projections,
     * created when first needed. The UPS projections are arbitrarily given zone numbers
     * {@value #POLAR} and -{@value #POLAR} for North and South poles respectively.
     *
     * <p>All accesses to this map shall be synchronized on {@code cachedProjections}.</p>
     *
     * @see #universal(double, double)
     */
    private final Map<Integer,ProjectedCRS> cachedProjections;

    /**
     * The special zone number used as key in {@link #cachedProjections} for polar stereographic projections.
     * Must be outside the range of UTM zone numbers.
     */
    private static final int POLAR = 90;

    /**
     * Creates a new constant for the given EPSG or SIS codes.
     *
     * @param geographic  the EPSG code for the two-dimensional geographic CRS.
     * @param geo3D       the EPSG code of the three-dimensional geographic CRS, or 0 if none.
     * @param geocentric  the EPSG code of the geocentric CRS, or 0 if none.
     * @param datum       the EPSG code for the datum.
     * @param ellipsoid   the EPSG code for the ellipsoid.
     */
    private CommonCRS(final short geographic, final short geo3D, final short geocentric, final short datum, final short ellipsoid,
            final short northUPS, final short southUPS, final short northUTM, final short southUTM, final byte firstZone, final byte lastZone)
    {
        this.geographic = geographic;
        this.geocentric = geocentric;
        this.geo3D      = geo3D;
        this.datum      = datum;
        this.ellipsoid  = ellipsoid;
        this.northUPS   = northUPS;
        this.southUPS   = southUPS;
        this.northUTM   = northUTM;
        this.southUTM   = southUTM;
        this.firstZone  = firstZone;
        this.lastZone   = lastZone;
        cachedProjections = new HashMap<>();
    }

    /**
     * Registers a listeners to be invoked when the classpath changed.
     * This will clear the cache, since the EPSG database may have changed.
     */
    static {
        SystemListener.add(new SystemListener(Modules.REFERENCING) {
            @Override protected void classpathChanged() {
                for (final CommonCRS e : values()) {
                    e.clear();
                }
            }
        });
    }

    /**
     * Invoked by when the cache needs to be cleared after a classpath change.
     */
    @SuppressWarnings("NestedSynchronizedStatement")    // Safe because cachedProjections never call any method of 'this'.
    final synchronized void clear() {
        cached           = null;
        cachedGeo3D      = null;
        cachedNormalized = null;
        cachedGeocentric = null;
        synchronized (cachedProjections) {
            cachedProjections.clear();
        }
    }

    /**
     * Returns the {@code CommonCRS} enumeration value for the datum of the given CRS.
     * The given CRS shall comply to the following conditions
     * (otherwise an {@link IllegalArgumentException} is thrown):
     *
     * <ul>
     *   <li>The {@code crs} is either an instance of {@link SingleCRS},
     *       or an instance of {@link org.opengis.referencing.crs.CompoundCRS}
     *       with an {@linkplain CRS#getHorizontalComponent horizontal component}.</li>
     *   <li>The {@code crs} or the horizontal component of {@code crs} is associated to a {@link GeodeticDatum}.</li>
     *   <li>The geodetic datum either<ul>
     *     <li>has the same EPSG code than one of the {@code CommonCRS} enumeration values, or</li>
     *     <li>has no EPSG code but is {@linkplain Utilities#equalsIgnoreMetadata equal, ignoring metadata},
     *       to the {@link #datum()} value of one of the {@code CommonCRS} enumeration values.</li>
     *   </ul></li>
     * </ul>
     *
     * This method is useful for easier creation of various coordinate reference systems through the
     * {@link #geographic()}, {@link #geocentric()} or other convenience methods when the set of datums
     * supported by {@code CommonCRS} is known to be sufficient.
     *
     * @param  crs  the coordinate reference system for which to get a {@code CommonCRS} value.
     * @return the {@code CommonCRS} value for the geodetic datum of the given CRS.
     * @throws IllegalArgumentException if no {@code CommonCRS} value can be found for the given CRS.
     *
     * @see #datum()
     * @since 0.8
     */
    public static CommonCRS forDatum(final CoordinateReferenceSystem crs) {
        final SingleCRS single;
        if (crs instanceof SingleCRS) {
            single = (SingleCRS) crs;
        } else {
            single = CRS.getHorizontalComponent(crs);
            if (single == null) {
                throw new IllegalArgumentException(Resources.format(
                        Resources.Keys.NonHorizontalCRS_1, IdentifiedObjects.getDisplayName(crs, null)));
            }
        }
        final Datum datum = single.getDatum();
        if (datum instanceof GeodeticDatum) {
            final CommonCRS c = forDatum((GeodeticDatum) datum);
            if (c != null) return c;
        }
        throw new IllegalArgumentException(Errors.format(
                Errors.Keys.UnsupportedDatum_1, IdentifiedObjects.getDisplayName(datum, null)));
    }

    /**
     * Returns the {@code CommonCRS} enumeration value for the given datum, or {@code null} if none.
     */
    static CommonCRS forDatum(final GeodeticDatum datum) {
        /*
         * First, try to search using only the EPSG code. This approach avoid initializing unneeded
         * geodetic objects (such initializations are costly if they require connection to the EPSG
         * database).
         */
        int epsg = 0;
        final Identifier identifier = IdentifiedObjects.getIdentifier(datum, Citations.EPSG);
        if (identifier != null) {
            final String code = identifier.getCode();
            if (code != null) try {
                epsg = Integer.parseInt(code);
            } catch (NumberFormatException e) {
                Logging.recoverableException(AuthorityFactories.LOGGER, CommonCRS.class, "forDatum", e);
            }
        }
        for (final CommonCRS c : values()) {
            if ((epsg != 0) ? c.datum == epsg : Utilities.equalsIgnoreMetadata(c.datum(), datum)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the default two-dimensional normalized geographic CRS.
     * The CRS returned by this method has the following properties:
     *
     * <ul>
     *   <li>Axis order is (<var>longitude</var>, <var>latitude</var>).</li>
     *   <li>Axis directions are ({@linkplain AxisDirection#EAST East}, {@linkplain AxisDirection#NORTH North}).</li>
     *   <li>Angular unit is {@link Units#DEGREE}.</li>
     *   <li>Prime meridian in Greenwich.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This method makes no guarantees about the datum. The current default datum is WGS 84,
     * but this may change in future SIS versions if a WGS 84 replacement become in wide use.</div>
     *
     * This default CRS is assigned to
     * {@linkplain org.apache.sis.geometry.GeneralEnvelope#GeneralEnvelope(org.opengis.metadata.extent.GeographicBoundingBox)
     * envelopes created from a geographic bounding box}.
     * Since ISO 19115 {@link org.opengis.metadata.extent.GeographicBoundingBox} is approximated by definition,
     * their datum can be arbitrary.
     *
     * @return the default two-dimensional geographic CRS with (<var>longitude</var>, <var>latitude</var>) axis order.
     */
    public static GeographicCRS defaultGeographic() {
        return DEFAULT.normalizedGeographic();
    }

    /**
     * Returns a two-dimensional geographic CRS with axes in the non-standard but computationally convenient
     * (<var>longitude</var>, <var>latitude</var>) order. The coordinate system axes will be oriented toward
     * {@linkplain AxisDirection#EAST East} and {@linkplain AxisDirection#NORTH North} respectively, with units
     * in degrees. The following table summarizes the coordinate reference systems known to this class,
     * together with an enumeration value that can be used for fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geographic CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>Code</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td></td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td></td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td>CRS:27</td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td>CRS:83</td></tr>
     *   <tr><td>GRS 1980</td>                 <td>{@link #GRS1980}</td><td></td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td></td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>CRS:84</td></tr>
     * </table></blockquote>
     *
     * @return the geographic CRS with non-standard (<var>longitude</var>, <var>latitude</var>) axis order.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeographicCRS#forConvention(AxesConvention)
     * @see AxesConvention#NORMALIZED
     */
    public GeographicCRS normalizedGeographic() {
        GeographicCRS object = cachedNormalized;
        if (object == null) {
            DefaultGeographicCRS crs = DefaultGeographicCRS.castOrCopy(geographic());
            crs = crs.forConvention(AxesConvention.RIGHT_HANDED);       // Equivalent to NORMALIZED in our cases, but faster.
            synchronized (this) {
                object = cachedNormalized;
                if (object == null) {
                    cachedNormalized = object = crs;
                }
            }
        }
        return object;
    }

    /**
     * Returns the two-dimensional geographic CRS with axes in the standard (<var>latitude</var>, <var>longitude</var>)
     * order. The coordinate system axes will be oriented toward {@linkplain AxisDirection#NORTH North} and
     * {@linkplain AxisDirection#EAST East} respectively, with units in degrees.
     * The following table summarizes the coordinate reference systems known to this class,
     * together with an enumeration value that can be used for fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geographic CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td>4230</td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td>4258</td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td>4267</td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td>4269</td></tr>
     *   <tr><td>GRS 1980</td>                 <td>{@link #GRS1980}</td><td><del>4019</del></td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td><del>4047</del></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td>4322</td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>4326</td></tr>
     * </table></blockquote>
     *
     * @return the geographic CRS with standard (<var>latitude</var>, <var>longitude</var>) axis order.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeographicCRS
     */
    public GeographicCRS geographic() {
        GeographicCRS object = geographic(cached);
        if (object == null) {
            synchronized (this) {
                object = geographic(cached);
                if (object == null) {
                    final GeodeticAuthorityFactory factory = factory();
                    if (factory != null) try {
                        cached = object = factory.createGeographicCRS(String.valueOf(geographic));
                        return object;
                    } catch (FactoryException e) {
                        failure(this, "geographic", e, geographic);
                    }
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6422.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    final EllipsoidalCS cs;
                    if (this == DEFAULT) {
                        cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem(StandardDefinitions.ELLIPSOIDAL_2D, true);
                    } else {
                        cs = DEFAULT.geographic().getCoordinateSystem();
                    }
                    object = StandardDefinitions.createGeographicCRS(geographic, datum(), cs);
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the three-dimensional geographic CRS with axes in the standard (<var>latitude</var>,
     * <var>longitude</var>, <var>height</var>) order. The following table summarizes the coordinate
     * reference systems known to this class, together with an enumeration value that can be used for
     * fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geographic CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td></td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td>4937</td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td></td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td></td></tr>
     *   <tr><td>GRS 1980</td>                 <td>{@link #GRS1980}</td><td></td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td>4985</td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>4979</td></tr>
     * </table></blockquote>
     *
     * @return the three-dimensional geographic CRS associated to this enum.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeographicCRS
     */
    public GeographicCRS geographic3D() {
        GeographicCRS object = cachedGeo3D;
        if (object == null) {
            synchronized (this) {
                object = cachedGeo3D;
                if (object == null) {
                    if (geo3D != 0) {
                        final GeodeticAuthorityFactory factory = factory();
                        if (factory != null) try {
                            cachedGeo3D = object = factory.createGeographicCRS(String.valueOf(geo3D));
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "geographic3D", e, geo3D);
                        }
                    }
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6423.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    final EllipsoidalCS cs;
                    if (this == DEFAULT) {
                        cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem(StandardDefinitions.ELLIPSOIDAL_3D, true);
                    } else {
                        cs = DEFAULT.geographic3D().getCoordinateSystem();
                    }
                    // Use same name and datum than the geographic CRS.
                    final GeographicCRS base = geographic();
                    object = new DefaultGeographicCRS(properties(base, geo3D), base.getDatum(), cs);
                    cachedGeo3D = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geocentric CRS using a Cartesian coordinate system. Axis units are metres.
     * The following table summarizes the coordinate reference systems known to this class,
     * together with an enumeration value that can be used for fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geocentric CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td></td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td>4936</td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td></td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td></td></tr>
     *   <tr><td>GRS 1980</td>                 <td>{@link #GRS1980}</td><td></td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td>4984</td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>4978</td></tr>
     * </table></blockquote>
     *
     * @return the geocentric CRS associated to this enum.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeocentricCRS
     */
    public GeocentricCRS geocentric() {
        GeocentricCRS object = cachedGeocentric;
        if (object == null) {
            synchronized (this) {
                object = cachedGeocentric;
                if (object == null) {
                    if (geocentric != 0) {
                        final GeodeticAuthorityFactory factory = factory();
                        if (factory != null) try {
                            cachedGeocentric = object = factory.createGeocentricCRS(String.valueOf(geocentric));
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "geocentric", e, geocentric);
                        }
                    }
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6500.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    final CartesianCS cs;
                    if (this == DEFAULT) {
                        cs = (CartesianCS) StandardDefinitions.createCoordinateSystem(StandardDefinitions.EARTH_CENTRED, true);
                    } else {
                        cs = (CartesianCS) DEFAULT.geocentric().getCoordinateSystem();
                    }
                    // Use same name and datum than the geographic CRS.
                    final GeographicCRS base = geographic();
                    object = new DefaultGeocentricCRS(properties(base, geocentric), base.getDatum(), cs);
                    cachedGeocentric = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geocentric CRS using a spherical coordinate system. Axes are:
     *
     * <ol>
     *   <li>Spherical latitude in degrees oriented toward {@linkplain AxisDirection#NORTH north}.</li>
     *   <li>Spherical longitude in degrees oriented toward {@linkplain AxisDirection#EAST east}.</li>
     *   <li>Geocentric radius in metres oriented toward {@linkplain AxisDirection#UP up}.</li>
     * </ol>
     *
     * @return the geocentric CRS associated to this enum.
     *
     * @see DefaultGeocentricCRS
     *
     * @since 0.7
     */
    public GeocentricCRS spherical() {
        GeocentricCRS object = cachedSpherical;
        if (object == null) {
            synchronized (this) {
                object = cachedSpherical;
                if (object == null) {
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6404.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    SphericalCS cs = null;
                    if (this == DEFAULT) {
                        final GeodeticAuthorityFactory factory = factory();
                        if (factory != null) try {
                            cs = factory.createSphericalCS(Short.toString(StandardDefinitions.SPHERICAL));
                        } catch (FactoryException e) {
                            failure(this, "spherical", e, StandardDefinitions.SPHERICAL);
                        }
                        if (cs == null) {
                            cs = (SphericalCS) StandardDefinitions.createCoordinateSystem(StandardDefinitions.SPHERICAL, true);
                        }
                    } else {
                        cs = (SphericalCS) DEFAULT.spherical().getCoordinateSystem();
                    }
                    // Use same name and datum than the geographic CRS.
                    final GeographicCRS base = geographic();
                    object = new DefaultGeocentricCRS(IdentifiedObjects.getProperties(base, exclude()), base.getDatum(), cs);
                    cachedSpherical = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geodetic datum associated to this geodetic object.
     * The following table summarizes the datums known to this class,
     * together with an enumeration value that can be used for fetching that datum:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geodetic datums</caption>
     *   <tr><th>Name or alias</th>                                     <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>European Datum 1950</td>                               <td>{@link #ED50}</td>   <td>6230</td></tr>
     *   <tr><td>European Terrestrial Reference System 1989</td>        <td>{@link #ETRS89}</td> <td>6258</td></tr>
     *   <tr><td>North American Datum 1927</td>                         <td>{@link #NAD27}</td>  <td>6267</td></tr>
     *   <tr><td>North American Datum 1983</td>                         <td>{@link #NAD83}</td>  <td>6269</td></tr>
     *   <tr><td>Not specified (based on GRS 1980 ellipsoid)</td>       <td>{@link #GRS1980}</td><td>6019</td></tr>
     *   <tr><td>Not specified (based on GRS 1980 Authalic Sphere)</td> <td>{@link #SPHERE}</td> <td>6047</td></tr>
     *   <tr><td>World Geodetic System 1972</td>                        <td>{@link #WGS72}</td>  <td>6322</td></tr>
     *   <tr><td>World Geodetic System 1984</td>                        <td>{@link #WGS84}</td>  <td>6326</td></tr>
     * </table></blockquote>
     *
     * @return the geodetic datum associated to this enum.
     *
     * @see #forDatum(CoordinateReferenceSystem)
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     */
    public GeodeticDatum datum() {
        GeodeticDatum object = datum(cached);
        if (object == null) {
            synchronized (this) {
                object = datum(cached);
                if (object == null) {
                    final GeodeticAuthorityFactory factory = factory();
                    if (factory != null) try {
                        cached = object = factory.createGeodeticDatum(String.valueOf(datum));
                        return object;
                    } catch (FactoryException e) {
                        failure(this, "datum", e, datum);
                    }
                    object = StandardDefinitions.createGeodeticDatum(datum, ellipsoid(), primeMeridian());
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the ellipsoid associated to this geodetic object.
     * The following table summarizes the ellipsoids known to this class,
     * together with an enumeration value that can be used for fetching that ellipsoid:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used ellipsoids</caption>
     *   <tr><th>Name or alias</th>                    <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>Clarke 1866</td>                      <td>{@link #NAD27}</td>  <td>7008</td></tr>
     *   <tr><td>GRS 1980</td>                         <td>{@link #GRS1980}</td><td>7019</td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td>         <td>{@link #SPHERE}</td> <td>7048</td></tr>
     *   <tr><td>International 1924</td>               <td>{@link #ED50}</td>   <td>7022</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1972</td> <td>{@link #WGS72}</td>  <td>7043</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1984</td> <td>{@link #WGS84}</td>  <td>7030</td></tr>
     * </table></blockquote>
     *
     * @return the ellipsoid associated to this enum.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     */
    public Ellipsoid ellipsoid() {
        Ellipsoid object = ellipsoid(cached);
        if (object == null) {
            synchronized (this) {
                object = ellipsoid(cached);
                if (object == null) {
                    if (this == NAD83) {
                        object = ETRS89.ellipsoid();            // Share the same instance for NAD83 and ETRS89.
                    } else {
                        final GeodeticAuthorityFactory factory = factory();
                        if (factory != null) try {
                            cached = object = factory.createEllipsoid(String.valueOf(ellipsoid));
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "ellipsoid", e, ellipsoid);
                        }
                        object = StandardDefinitions.createEllipsoid(ellipsoid);
                    }
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the prime meridian associated to this geodetic object.
     * The following table summarizes the prime meridians known to this class,
     * together with an enumeration value that can be used for fetching that prime meridian:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used prime meridians</caption>
     *   <tr><th>Name or alias</th> <th>Enum</th>           <th>EPSG</th></tr>
     *   <tr><td>Greenwich</td>     <td>{@link #WGS84}</td> <td>8901</td></tr>
     * </table></blockquote>
     *
     * @return the prime meridian associated to this enum.
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     */
    public PrimeMeridian primeMeridian() {
        PrimeMeridian object = primeMeridian(cached);
        if (object == null) {
            synchronized (this) {
                object = primeMeridian(cached);
                if (object == null) {
                    if (this != DEFAULT) {
                        object = DEFAULT.primeMeridian();           // Share the same instance for all constants.
                    } else {
                        final GeodeticAuthorityFactory factory = factory();
                        if (factory != null) try {
                            cached = object = factory.createPrimeMeridian(StandardDefinitions.GREENWICH);
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "primeMeridian", e, Constants.EPSG_GREENWICH);
                        }
                        object = StandardDefinitions.primeMeridian();
                    }
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geographic CRS associated to the given object, or {@code null} if none.
     */
    private static GeographicCRS geographic(final IdentifiedObject object) {
        return (object instanceof GeographicCRS) ? (GeographicCRS) object : null;
    }

    /**
     * Returns the datum associated to the given object, or {@code null} if none.
     */
    private static GeodeticDatum datum(final IdentifiedObject object) {
        if (object instanceof GeodeticDatum) {
            return (GeodeticDatum) object;
        }
        if (object instanceof GeodeticCRS) {
            return ((GeodeticCRS) object).getDatum();
        }
        return null;
    }

    /**
     * Returns the ellipsoid associated to the given object, or {@code null} if none.
     */
    private static Ellipsoid ellipsoid(final IdentifiedObject object) {
        if (object instanceof Ellipsoid) {
            return (Ellipsoid) object;
        }
        final GeodeticDatum datum = datum(object);
        return (datum != null) ? datum.getEllipsoid() : null;
    }

    /**
     * Returns the prime meridian associated to the given object, or {@code null} if none.
     */
    private static PrimeMeridian primeMeridian(final IdentifiedObject object) {
        if (object instanceof PrimeMeridian) {
            return (PrimeMeridian) object;
        }
        final GeodeticDatum datum = datum(object);
        return (datum != null) ? datum.getPrimeMeridian() : null;
    }

    /*
     * NOTE ABOUT MAP PROJECTION CONVENIENCE METHODS:
     * There is no convenience method for projections other than UTM because this enumeration is not a
     * factory for arbitrary CRS (the UTM projection has the advantage of being constrained to zones).
     * World-wide projections like "WGS 84 / World Mercator" are not handled neither because they make
     * sense only for some datum like WGS84 or WGS72. Application to more regional datum like NAD27 or
     * ED50 would be more questionable.
     */

    /**
     * Returns a Universal Transverse Mercator (UTM) or a Universal Polar Stereographic (UPS) projection
     * for the zone containing the given point.
     * There is a total of 120 UTM zones, with 60 zones in the North hemisphere and 60 zones in the South hemisphere.
     * The projection zone is determined from the arguments as below:
     *
     * <ul class="verbose">
     *   <li>If the <var>latitude</var> argument is less than 80°S or equal or greater than 84°N,
     *       then a <cite>Universal Polar Stereographic</cite> projection is created.</li>
     *   <li>Otherwise a <cite>Universal Transverse Mercator</cite> projection is created as below:
     *     <ul class="verbose">
     *       <li>The sign of the <var>latitude</var> argument determines the hemisphere:
     *           North for positive latitudes (including positive zero) or
     *           South for negative latitudes (including negative zero).
     *           The latitude magnitude is ignored, except for the special cases documented below
     *           and for ensuring that the latitude is inside the [-90 … 90]° range.</li>
     *       <li>The value of the <var>longitude</var> argument determines the 6°-width zone,
     *           numbered from 1 for the zone starting at 180°W up to 60 for the zone finishing at 180°E.
     *           Longitudes outside the [-180 … 180]° range will be rolled as needed before to compute the zone.</li>
     *       <li>Calculation of UTM zone involves two special cases (if those special cases are not desired,
     *           they can be avoided by making sure that the given latitude is below 56°N):
     *         <ul>
     *           <li>Between 56°N and 64°N, zone 32 is widened to 9° (at the expense of zone 31)
     *               to accommodate southwest Norway.</li>
     *           <li>Between 72°N and 84°N, zones 33 and 35 are widened to 12° to accommodate Svalbard.
     *               To compensate for these 12° wide zones, zones 31 and 37 are widened to 9° and
     *               zones 32, 34, and 36 are eliminated.</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <div class="note"><b>Tip:</b>
     * for "straight" UTM zone calculation without any special case (neither Norway, Svalbard or Universal Polar
     * Stereographic projection), one can replace the {@code latitude} argument by {@code Math.signum(latitude)}.
     * For using a specific zone number, one can additionally replace the {@code longitude} argument by
     * {@code zone * 6 - 183}.</div>
     *
     * The map projection uses the following parameters:
     *
     * <table class="sis">
     *   <caption>Universal Transverse Mercator (UTM) and Universal Polar Stereographic (UPS) projection parameters</caption>
     *   <tr>
     *     <th>Parameter name</th>
     *     <th>UTM parameter value</th>
     *     <th>UPS parameter value</th>
     *   </tr><tr>
     *     <td>Latitude of natural origin</td>
     *     <td>0°</td>
     *     <td>90°N or 90°S depending on the sign of given latitude</td>
     *   </tr><tr>
     *     <td>Longitude of natural origin</td>
     *     <td>Central meridian of the UTM zone containing the given longitude</td>
     *     <td>0°</td>
     *   </tr><tr>
     *     <td>Scale factor at natural origin</td>
     *     <td>0.9996</td>
     *     <td>0.994</td>
     *   </tr><tr>
     *     <td>False easting</td>
     *     <td>500 000 metres</td>
     *     <td>2 000 000 metres</td>
     *   </tr><tr>
     *     <td>False northing</td>
     *     <td>0 (North hemisphere) or 10 000 000 (South hemisphere) metres</td>
     *     <td>2 000 000 metres</td>
     *   </tr>
     * </table>
     *
     * The coordinate system axes are (Easting, Northing) in metres.
     *
     * <p>Be aware of parameter order! For this method, latitude is first.
     * This order is for consistency with the non-normalized {@linkplain #geographic() geographic} CRS
     * of all items in this {@code CommonCRS} enumeration.</p>
     *
     * @param  latitude  a latitude in the desired UTM or UPS projection zone.
     * @param  longitude a longitude in the desired UTM or UPS projection zone.
     * @return a Universal Transverse Mercator or Polar Stereographic projection for the zone containing the given point.
     *
     * @since 0.8
     */
    public ProjectedCRS universal(final double latitude, final double longitude) {
        ArgumentChecks.ensureBetween("latitude",   Latitude.MIN_VALUE,     Latitude.MAX_VALUE,     latitude);
        ArgumentChecks.ensureBetween("longitude", -Formulas.LONGITUDE_MAX, Formulas.LONGITUDE_MAX, longitude);
        final boolean isSouth = MathFunctions.isNegative(latitude);
        final boolean isUTM   = latitude >= TransverseMercator.Zoner.SOUTH_BOUNDS
                             && latitude <  TransverseMercator.Zoner.NORTH_BOUNDS;
        final int zone = isUTM ? TransverseMercator.Zoner.UTM.zone(latitude, longitude) : POLAR;
        final Integer key = isSouth ? -zone : zone;
        ProjectedCRS crs;
        synchronized (cachedProjections) {
            crs = cachedProjections.get(key);
        }
        if (crs == null) {
            /*
             * Requested CRS has not been previously created, or the cache has been cleared.
             * Before to create the CRS explicitly, try to get it from the EPSG database.
             * Using the EPSG geodetic dataset when possible gives us more information,
             * like the aliases and area of validity.
             */
            int code = 0;
            if (!isUTM) {
                code = Short.toUnsignedInt(isSouth ? southUPS : northUPS);
            } else if (zone >= firstZone && zone <= lastZone) {
                code = Short.toUnsignedInt(isSouth ? southUTM : northUTM);
            }
            if (code != 0) {
                if (isUTM) code += zone;
                final GeodeticAuthorityFactory factory = factory();
                if (factory != null) try {
                    return factory.createProjectedCRS(String.valueOf(code));
                } catch (FactoryException e) {
                    failure(this, "universal", e, code);
                }
            }
            /*
             * At this point we couldn't use the EPSG dataset; we have to create the CRS ourselves.
             * All constants defined in this enumeration use the same coordinate system (EPSG:4400)
             * except for the polar regions. We will arbitrarily create the CS only for a frequently
             * used datum, then share that CS instance for all other constants.
             */
            CartesianCS cs = null;
            if (isUTM) {
                synchronized (DEFAULT.cachedProjections) {
                    for (final Map.Entry<Integer,ProjectedCRS> entry : DEFAULT.cachedProjections.entrySet()) {
                        if (Math.abs(entry.getKey()) != POLAR) {
                            cs = entry.getValue().getCoordinateSystem();
                            break;
                        }
                    }
                }
            }
            /*
             * If we didn't found a Coordinate System for EPSG:4400, or if the CS that we needed was
             * for another EPSG code (polar cases), delegate to the WGS84 datum or create the CS now.
             *
             *   EPSG:4400 — Cartesian 2D CS. Axes: easting, northing (E,N). Orientations: east, north. UoM: m.
             *   EPSG:1026 — Cartesian 2D CS for UPS north. Axes: E,N. Orientations: E along 90°E meridian, N along 180°E meridian. UoM: m.
             *   EPSG:1027 — Cartesian 2D CS for UPS south. Axes: E,N. Orientations: E along 90°E, N along 0°E meridians. UoM: m.
             */
            if (cs == null) {
                if (this != DEFAULT) {
                    cs = DEFAULT.universal(latitude, longitude).getCoordinateSystem();
                } else {
                    cs = (CartesianCS) StandardDefinitions.createCoordinateSystem(
                            isUTM   ? StandardDefinitions.CARTESIAN_2D :
                            isSouth ? StandardDefinitions.UPS_SOUTH
                                    : StandardDefinitions.UPS_NORTH, true);
                }
            }
            crs = StandardDefinitions.createUniversal(code, geographic(), isUTM, latitude, longitude, cs);
            final ProjectedCRS other;
            synchronized (cachedProjections) {
                other = cachedProjections.putIfAbsent(key, crs);
            }
            if (other != null) {
                return other;
            }
        }
        return crs;
    }




    /**
     * Frequently-used vertical CRS and datum that are guaranteed to be available in SIS.
     * Methods in this enumeration are shortcuts for object definitions in the EPSG database.
     * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
     * then {@code Vertical} fallback on hard-coded values. Consequently, those methods never return {@code null}.
     *
     * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code Vertical}
     * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
     * (e.g. the application is running in a container environment and some modules have been installed or uninstalled).</p>
     *
     * <p><b>Example:</b> the following code fetches a vertical Coordinate Reference System for heights
     * above the Mean Sea Level (MSL):</p>
     *
     * {@snippet lang="java" :
     *     VerticalCRS crs = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
     *     }
     *
     * Below is an alphabetical list of object names available in this enumeration:
     *
     * <blockquote><table class="sis">
     *   <caption>Geodetic objects accessible by enumeration constants</caption>
     *   <tr><th>Name or alias</th>                      <th>Object type</th> <th>Enumeration value</th></tr>
     *   <tr><td>Barometric altitude</td>                <td>CRS, Datum</td>  <td>{@link #BAROMETRIC}</td></tr>
     *   <!-- <del>Ellipsoidal height</del> intentionally omitted             <td><del>{@link #ELLIPSOIDAL}</del></td> -->
     *   <tr><td>Mean Sea Level</td>                     <td>Datum</td>       <td>{@link #MEAN_SEA_LEVEL}</td></tr>
     *   <tr><td>Mean Sea Level depth</td>               <td>CRS</td>         <td>{@link #DEPTH}</td></tr>
     *   <tr><td>Mean Sea Level height</td>              <td>CRS</td>         <td>{@link #MEAN_SEA_LEVEL}</td></tr>
     *   <tr><td>NAVD88 height</td>                      <td>CRS</td>         <td>{@link #NAVD88}</td></tr>
     *   <tr><td>North American Vertical Datum 1988</td> <td>Datum</td>       <td>{@link #NAVD88}</td></tr>
     *   <tr><td>Other surface</td>                      <td>CRS, Datum</td>  <td>{@link #OTHER_SURFACE}</td></tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Note:</b>
     * We do not provide a {@code GEOIDAL} value because its definition depends on the realization epoch.
     * For example, EGM84, EGM96 and EGM2008 are applications of three different geoid models on the WGS 84 ellipsoid.
     * The {@link #MEAN_SEA_LEVEL} value can be used instead as an approximation of geoidal heights.</div>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.3
     *
     * @see org.apache.sis.referencing.factory.CommonAuthorityFactory
     *
     * @since 0.4
     */
    public enum Vertical {
        /**
         * Height measured by atmospheric pressure in hectopascals (hPa).
         * Hectopascals are the units of measurement used by the worldwide meteorological community.
         * The datum is not specific to any location or epoch.
         *
         * @see VerticalDatumType#BAROMETRIC
         */
        BAROMETRIC(false, Vocabulary.Keys.BarometricAltitude, Vocabulary.Keys.ConstantPressureSurface),

        /**
         * Height measured above the Mean Sea Level (MSL) in metres. Can be used as an approximation of geoidal heights
         * (height measured above an equipotential surface), except that MSL are not specific to any location or epoch.
         *
         * <blockquote><table class="compact">
         * <caption>Mean Sea Level properties</caption>
         *   <tr><th>CRS identifiers:</th>      <td>EPSG:5714</td></tr>
         *   <tr><th>Name or abbreviation:</th> <td>Mean Sea Level (MSL) height</td></tr>
         *   <tr><th>Direction:</th>            <td>{@link AxisDirection#UP}</td></tr>
         *   <tr><th>Unit:</th>                 <td>{@link Units#METRE}</td></tr>
         * </table></blockquote>
         *
         * @see VerticalDatumType#GEOIDAL
         */
        MEAN_SEA_LEVEL(true, (short) 5714, (short) 5100),

        /**
         * Depth measured below the Mean Sea Level (MSL) in metres.
         *
         * <blockquote><table class="compact">
         * <caption>Depth properties</caption>
         *   <tr><th>CRS identifiers:</th>      <td>EPSG:5715</td></tr>
         *   <tr><th>Name or abbreviation:</th> <td>Mean Sea Level depth (MSL) depth</td></tr>
         *   <tr><th>Direction:</th>            <td>{@link AxisDirection#DOWN}</td></tr>
         *   <tr><th>Unit:</th>                 <td>{@link Units#METRE}</td></tr>
         * </table></blockquote>
         *
         * @see VerticalDatumType#GEOIDAL
         */
        DEPTH(true, (short) 5715, (short) 5100),

        /**
         * North American Vertical Datum 1988 height.
         *
         * <blockquote><table class="compact">
         * <caption>NAVD88 properties</caption>
         *   <tr><th>CRS identifier:</th>       <td>CRS:88, EPSG:5703</td></tr>
         *   <tr><th>Name or abbreviation:</th> <td>North American Vertical Datum 1988 (NAVD88) height</td></tr>
         *   <tr><th>Direction:</th>            <td>{@link AxisDirection#UP}</td></tr>
         *   <tr><th>Unit:</th>                 <td>{@link Units#METRE}</td></tr>
         * </table></blockquote>
         *
         * @see CommonCRS#NAD83
         *
         * @since 0.7
         */
        NAVD88(true, (short) 5703, (short) 5103),

        /**
         * Height measured along the normal to the ellipsoid used in the definition of horizontal datum.
         * The unit of measurement is metres.
         *
         * <p><b>Ellipsoidal height is not part of ISO 19111 international standard.</b>
         * Such vertical CRS is usually not recommended since ellipsoidal heights make little sense without
         * their (<var>latitude</var>, <var>longitude</var>) locations. The ISO specification defines instead
         * three-dimensional {@code GeographicCRS} for that reason. Users are encouraged to avoid this orphan
         * ellipsoidal height as much as possible.</p>
         */
        ELLIPSOIDAL(false, Vocabulary.Keys.EllipsoidalHeight, Vocabulary.Keys.Ellipsoid),

        /**
         * Height measured above other kind of surface, for example a geological feature.
         * The unit of measurement is metres.
         *
         * @see VerticalDatumType#OTHER_SURFACE
         */
        OTHER_SURFACE(false, Vocabulary.Keys.Height, Vocabulary.Keys.OtherSurface);

        /**
         * {@code true} if {@link #crs} and {@link #datum} are EPSG codes, or {@code false} if
         * they are resource keys for the name as one of the {@code Vocabulary.Keys} constants.
         */
        final boolean isEPSG;

        /**
         * The EPSG code for the CRS or the resource keys, depending on {@link #isEPSG} value.
         */
        final short crs;

        /**
         * The EPSG code for the datum or the resource keys, depending on {@link #isEPSG} value.
         */
        final short datum;

        /**
         * The cached object. This is initially {@code null}, then set to various kinds of objects depending
         * on which method has been invoked. The kind of object stored in this field may change during the
         * application execution.
         */
        private transient volatile IdentifiedObject cached;

        /**
         * Creates a new enumeration value of the given name.
         *
         * <h4>API design note</h4>
         * This constructor does not expect {@link VerticalDatumType} constant in order to avoid too
         * early class initialization. In particular, we do not want early dependency to the SIS-specific
         * {@code VerticalDatumTypes.ELLIPSOIDAL} constant.
         */
        private Vertical(final boolean isEPSG, final short crs, final short datum) {
            this.isEPSG = isEPSG;
            this.crs    = crs;
            this.datum  = datum;
        }

        /**
         * Registers a listeners to be invoked when the classpath changed.
         * This will clear the cache, since the factories may have changed.
         */
        static {
            SystemListener.add(new SystemListener(Modules.REFERENCING) {
                @Override protected void classpathChanged() {
                    for (final Vertical e : values()) {
                        e.clear();
                    }
                }
            });
        }

        /**
         * Invoked by when the cache needs to be cleared after a classpath change.
         */
        final synchronized void clear() {
            cached = null;
        }

        /**
         * Returns the coordinate reference system associated to this vertical object.
         * The following table summarizes the CRS known to this class,
         * together with an enumeration value that can be used for fetching that CRS:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used vertical CRS</caption>
         *   <tr><th>Name or alias</th>             <th>Enum</th>                        <th>EPSG</th></tr>
         *   <tr><td>Barometric altitude</td>       <td>{@link #BAROMETRIC}</td>         <td></td></tr>
         *   <!-- <del>Ellipsoidal height</del> intentionally omitted -->
         *   <tr><td>Mean Sea Level depth</td>      <td>{@link #DEPTH}</td>              <td>5715</td></tr>
         *   <tr><td>Mean Sea Level height</td>     <td>{@link #MEAN_SEA_LEVEL}</td>     <td>5714</td></tr>
         *   <tr><td>Other surface</td>             <td>{@link #OTHER_SURFACE}</td>      <td></td></tr>
         * </table></blockquote>
         *
         * @return the CRS associated to this enum.
         *
         * @see DefaultVerticalCRS
         */
        public VerticalCRS crs() {
            VerticalCRS object = crs(cached);
            if (object == null) {
                synchronized (this) {
                    object = crs(cached);
                    if (object == null) {
                        if (isEPSG) {
                            final GeodeticAuthorityFactory factory = factory();
                            if (factory != null) try {
                                cached = object = factory.createVerticalCRS(String.valueOf(crs));
                                return object;
                            } catch (FactoryException e) {
                                failure(this, "crs", e, crs);
                            }
                            object = StandardDefinitions.createVerticalCRS(crs, datum());
                        } else {
                            final VerticalCS cs = cs();
                            object = new DefaultVerticalCRS(IdentifiedObjects.getProperties(cs, exclude()), datum(), cs);
                        }
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Creates the coordinate system associated to this vertical object.
         * This is used only for CRS not identified by an EPSG code.
         * This method does not cache the coordinate system.
         */
        private VerticalCS cs() {
            final Map<String,?> properties = properties(crs);
            final Unit<?> unit;
            switch (this) {
                default: {
                    unit = Units.METRE;
                    break;
                }
                case BAROMETRIC: {
                    unit = Units.HECTOPASCAL;
                    break;
                }
            }
            return new DefaultVerticalCS(properties,
                    new DefaultCoordinateSystemAxis(properties, "h", AxisDirection.UP, unit));
        }

        /**
         * Returns the datum associated to this vertical object.
         * The following table summarizes the datum known to this class,
         * together with an enumeration value that can be used for fetching that datum:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used vertical datum</caption>
         *   <tr><th>Name or alias</th>             <th>Enum</th>                        <th>EPSG</th></tr>
         *   <tr><td>Barometric altitude</td>       <td>{@link #BAROMETRIC}</td>         <td></td></tr>
         *   <!-- <del>Ellipsoidal height</del> intentionally omitted -->
         *   <tr><td>Mean Sea Level</td>            <td>{@link #MEAN_SEA_LEVEL}</td>     <td>5100</td></tr>
         *   <tr><td>Other surface</td>             <td>{@link #OTHER_SURFACE}</td>      <td></td></tr>
         * </table></blockquote>
         *
         * @return the datum associated to this enum.
         *
         * @see DefaultVerticalDatum
         */
        public VerticalDatum datum() {
            VerticalDatum object = datum(cached);
            if (object == null) {
                synchronized (this) {
                    object = datum(cached);
                    if (object == null) {
                        if (isEPSG) {
                            final GeodeticAuthorityFactory factory = factory();
                            if (factory != null) try {
                                cached = object = factory.createVerticalDatum(String.valueOf(datum));
                                return object;
                            } catch (FactoryException e) {
                                failure(this, "datum", e, datum);
                            }
                            object = StandardDefinitions.createVerticalDatum(datum);
                        } else {
                            object = new DefaultVerticalDatum(properties(datum), VerticalDatumType.valueOf(name()));
                        }
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Returns the vertical CRS associated to the given object, or {@code null} if none.
         */
        private static VerticalCRS crs(final IdentifiedObject object) {
            return (object instanceof VerticalCRS) ? (VerticalCRS) object : null;
        }

        /**
         * Returns the datum associated to the given object, or {@code null} if none.
         */
        private static VerticalDatum datum(final IdentifiedObject object) {
            if (object instanceof VerticalDatum) {
                return (VerticalDatum) object;
            }
            if (object instanceof VerticalCRS) {
                return ((VerticalCRS) object).getDatum();
            }
            return null;
        }
    }




    /**
     * Frequently-used temporal CRS and datum that are guaranteed to be available in SIS.
     *
     * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code Temporal}
     * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
     * (e.g. the application is running in a container environment and some modules have been installed or uninstalled).</p>
     *
     * <p><b>Example:</b> the following code fetches a temporal Coordinate Reference System using the Julian calendar:</p>
     *
     * {@snippet lang="java" :
     *     TemporalCRS crs = CommonCRS.Temporal.JULIAN.crs();
     *     }
     *
     * Below is an alphabetical list of object names available in this enumeration.
     * Note that the namespace of identifiers ("OGC" versus "SIS") may change in any future version.
     *
     * <blockquote><table class="sis">
     *   <caption>Temporal objects accessible by enumeration constants</caption>
     *   <tr><th>Name or alias</th>    <th>Identifier</th>                      <th>Object type</th> <th>Enumeration value</th></tr>
     *   <tr><td>Dublin Julian</td>    <td>{@code SIS:DublinJulian}</td>        <td>CRS, Datum</td>  <td>{@link #DUBLIN_JULIAN}</td></tr>
     *   <tr><td>Java time</td>        <td>{@code SIS:JavaTime}</td>            <td>CRS</td>         <td>{@link #JAVA}</td></tr>
     *   <tr><td>Julian</td>           <td>{@code OGC:JulianDate}</td>          <td>CRS, Datum</td>  <td>{@link #JULIAN}</td></tr>
     *   <tr><td>Modified Julian</td>  <td>{@code SIS:ModifiedJulianDate}</td>  <td>CRS, Datum</td>  <td>{@link #MODIFIED_JULIAN}</td></tr>
     *   <tr><td>Truncated Julian</td> <td>{@code OGC:TruncatedJulianDate}</td> <td>CRS, Datum</td>  <td>{@link #TRUNCATED_JULIAN}</td></tr>
     *   <tr><td>Unix/POSIX time</td>  <td>{@code OGC:UnixTime}</td>            <td>CRS, Datum</td>  <td>{@link #UNIX}</td></tr>
     * </table></blockquote>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.3
     *
     * @see Engineering#TIME
     *
     * @since 0.4
     */
    public enum Temporal {
        /**
         * Time measured as days since January 1st, 4713 BC at 12:00 UTC in proleptic Julian calendar.
         * This epoch is equivalent to November 24, 4714 BC when expressed in the proleptic Gregorian
         * calendar instead of the Julian one.
         *
         * <p><b>Note on dates formatting:</b>
         * the legacy date/time formatting classes in the {@link java.text} package uses the proleptic
         * Julian calendar for dates before October 15, 1582, while the new date/time formatting classes
         * in the {@link java.time.format} package use the ISO-8601 calendar system, which is equivalent
         * to the proleptic Gregorian calendar for every dates. For parsing and formatting of Julian days,
         * the {@link java.text.SimpleDateFormat} class is closer to the common practice (but not ISO 8601
         * compliant).</p>
         */
        JULIAN(Vocabulary.Keys.Julian, -2440588L * MILLISECONDS_PER_DAY + MILLISECONDS_PER_DAY/2,
               "JulianDate", true),

        /**
         * Time measured as days since November 17, 1858 at 00:00 UTC.
         * A <cite>Modified Julian day</cite> (MJD) is defined relative to
         * <cite>Julian day</cite> (JD) as {@code MJD = JD − 2400000.5}.
         */
        MODIFIED_JULIAN(Vocabulary.Keys.ModifiedJulian, -40587L * MILLISECONDS_PER_DAY,
                        "ModifiedJulianDate", false),

        /**
         * Time measured as days since May 24, 1968 at 00:00 UTC.
         * This epoch was introduced by NASA for the space program.
         * A <cite>Truncated Julian day</cite> (TJD) is defined relative to
         * <cite>Julian day</cite> (JD) as {@code TJD = JD − 2440000.5}.
         */
        TRUNCATED_JULIAN(Vocabulary.Keys.TruncatedJulian, -587L * MILLISECONDS_PER_DAY,
                         "TruncatedJulianDate", true),

        /**
         * Time measured as days since December 31, 1899 at 12:00 UTC.
         * A <cite>Dublin Julian day</cite> (DJD) is defined relative to
         * <cite>Julian day</cite> (JD) as {@code DJD = JD − 2415020}.
         */
        DUBLIN_JULIAN(Vocabulary.Keys.DublinJulian, -25568L * MILLISECONDS_PER_DAY + MILLISECONDS_PER_DAY/2,
                      "DublinJulian", false),

        /**
         * Time measured as seconds since January 1st, 1970 at 00:00 UTC.
         */
        UNIX(Vocabulary.Keys.Time_1, 0, "UnixTime", true),

        /**
         * Time measured as milliseconds since January 1st, 1970 at 00:00 UTC.
         */
        JAVA(Vocabulary.Keys.Time_1, 0, "JavaTime", false);

        /**
         * The resource keys for the name as one of the {@code Vocabulary.Keys} constants.
         */
        private final short key;

        /**
         * The date and time origin of this temporal datum.
         */
        private final long epoch;

        /**
         * Identifier in OGC or SIS namespace.
         *
         * @see org.apache.sis.referencing.factory.CommonAuthorityFactory#TEMPORAL_NAMES
         */
        private final String identifier;

        /**
         * Whether the identifier is in OGC namespace.
         */
        private final boolean isOGC;

        /**
         * The cached object. This is initially {@code null}, then set to various kinds of objects depending
         * on which method has been invoked. The kind of object stored in this field may change during the
         * application execution.
         */
        private transient volatile IdentifiedObject cached;

        /**
         * Creates a new enumeration value of the given name with time counted since the given epoch.
         */
        private Temporal(final short name, final long epoch, final String identifier, final boolean isOGC) {
            this.key        = name;
            this.epoch      = epoch;
            this.identifier = identifier;
            this.isOGC      = isOGC;
        }

        /**
         * Registers a listeners to be invoked when the classpath changed.
         * This will clear the cache, since the factories may have changed.
         */
        static {
            SystemListener.add(new SystemListener(Modules.REFERENCING) {
                @Override protected void classpathChanged() {
                    for (final Temporal e : values()) {
                        e.clear();
                    }
                }
            });
        }

        /**
         * Invoked by when the cache needs to be cleared after a classpath change.
         */
        final synchronized void clear() {
            cached = null;
        }

        /**
         * Returns the enumeration value for the given identifier (without namespace).
         * Identifiers in OGC namespace are {@code "JulianDate"}, {@code "TruncatedJulianDate"} and {@code "UnixTime"}.
         * Identifiers in SIS namespace are {@code "ModifiedJulianDate"}, {@code "DublinJulian"} and {@code "JavaTime"}.
         * Note that the content of OGC and SIS namespaces may change in any future version.
         *
         * @param  identifier  case-insensitive identifier of the desired temporal CRS, without namespace.
         * @param  onlyOGC     whether to return the CRS only if its identifier is in OGC namespace.
         * @return the enumeration value for the given identifier.
         * @throws IllegalArgumentException if the given identifier is not recognized.
         *
         * @see <a href="http://www.opengis.net/def/crs/OGC/0">OGC Definitions Server</a>
         *
         * @since 1.3
         */
        public static Temporal forIdentifier(final String identifier, final boolean onlyOGC) {
            ArgumentChecks.ensureNonEmpty("identifier", identifier);
            for (final Temporal candidate : values()) {
                if (candidate.identifier.equalsIgnoreCase(identifier)) {
                    if (onlyOGC & !candidate.isOGC) {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.IdentifierNotInNamespace_2, Constants.OGC, candidate.identifier));
                    }
                    return candidate;
                }
            }
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownEnumValue_2, Temporal.class, identifier));
        }

        /**
         * Returns the enumeration value for the given epoch, or {@code null} if none.
         * If the epoch is January 1st, 1970, then this method returns {@link #UNIX}.
         *
         * @param  epoch  the epoch for which to get an enumeration value, or {@code null}.
         * @return the enumeration value for the given epoch, or {@code null} if none.
         *
         * @since 1.0
         */
        @OptionalCandidate
        public static Temporal forEpoch(final Instant epoch) {
            if (epoch != null) {
                final long e = epoch.toEpochMilli();
                for (final Temporal candidate : values()) {
                    if (candidate.epoch == e) {
                        return candidate;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the coordinate reference system associated to this temporal object.
         * The following table summarizes the CRS known to this class,
         * together with an enumeration value that can be used for fetching that CRS:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used temporal CRS</caption>
         *   <tr><th>Name or alias</th>      <th>Enum</th></tr>
         *   <tr><td>Dublin Julian</td>      <td>{@link #DUBLIN_JULIAN}</td></tr>
         *   <tr><td>Julian</td>             <td>{@link #JULIAN}</td></tr>
         *   <tr><td>Modified Julian</td>    <td>{@link #MODIFIED_JULIAN}</td></tr>
         *   <tr><td>Truncated Julian</td>   <td>{@link #TRUNCATED_JULIAN}</td></tr>
         *   <tr><td>Unix/POSIX</td>         <td>{@link #UNIX}</td></tr>
         *   <tr><td>Java {@link Date}</td>  <td>{@link #JAVA}</td></tr>
         * </table></blockquote>
         *
         * @return the CRS associated to this enum.
         *
         * @see DefaultTemporalCRS
         */
        public TemporalCRS crs() {
            TemporalCRS object = crs(cached);
            if (object == null) {
                synchronized (this) {
                    object = crs(cached);
                    if (object == null) {
                        final TemporalDatum datum = datum();
                        final Map<String,?> source;
                        if (this == JAVA) {
                            source = properties(Vocabulary.formatInternational(key, "Java"));
                        } else {
                            source = IdentifiedObjects.getProperties(datum, exclude());
                        }
                        final Map<String,Object> properties = new HashMap<>(source);
                        properties.put(TemporalCRS.IDENTIFIERS_KEY,
                                new NamedIdentifier(isOGC ? Citations.OGC : Citations.SIS, identifier));
                        object = new DefaultTemporalCRS(properties, datum, cs());
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Creates the coordinate system associated to this temporal object.
         * This method does not cache the coordinate system.
         */
        @SuppressWarnings("fallthrough")
        private TimeCS cs() {
            final Map<String,?> cs, axis;
            Unit<Time> unit = Units.SECOND;
            switch (this) {
                default: {
                    // Share the coordinate system created for truncated Julian.
                    return TRUNCATED_JULIAN.crs().getCoordinateSystem();
                }
                case TRUNCATED_JULIAN: {
                    unit = Units.DAY;
                    // Fall through
                }
                case UNIX: {
                    // Share the NamedIdentifier created for Java time.
                    final TimeCS share = JAVA.crs().getCoordinateSystem();
                    cs   = IdentifiedObjects.getProperties(share, exclude());
                    axis = IdentifiedObjects.getProperties(share.getAxis(0), exclude());
                    break;
                }
                case JAVA: {
                    // Create all properties for a new coordinate system.
                    cs   = properties(Vocabulary.Keys.Temporal);
                    axis = properties(Vocabulary.Keys.Time);
                    unit = Units.MILLISECOND;
                    break;
                }
            }
            return new DefaultTimeCS(cs, new DefaultCoordinateSystemAxis(axis, "t", AxisDirection.FUTURE, unit));
        }

        /**
         * Returns the datum associated to this temporal object.
         * The following table summarizes the datum known to this class,
         * together with an enumeration value that can be used for fetching that datum:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used temporal datum</caption>
         *   <tr><th>Name or alias</th>      <th>Enum</th></tr>
         *   <tr><td>Dublin Julian</td>      <td>{@link #DUBLIN_JULIAN}</td></tr>
         *   <tr><td>Julian</td>             <td>{@link #JULIAN}</td></tr>
         *   <tr><td>Modified Julian</td>    <td>{@link #MODIFIED_JULIAN}</td></tr>
         *   <tr><td>Truncated Julian</td>   <td>{@link #TRUNCATED_JULIAN}</td></tr>
         *   <tr><td>Unix/POSIX or Java</td> <td>{@link #UNIX}</td></tr>
         * </table></blockquote>
         *
         * @return the datum associated to this enum.
         *
         * @see DefaultTemporalDatum
         */
        public TemporalDatum datum() {
            TemporalDatum object = datum(cached);
            if (object == null) {
                synchronized (this) {
                    object = datum(cached);
                    if (object == null) {
                        if (this == UNIX) {
                            object = JAVA.datum();          // Share the same instance for UNIX and JAVA.
                        } else {
                            final Map<String,?> properties;
                            if (key == Vocabulary.Keys.Time_1) {
                                properties = properties(Vocabulary.formatInternational(key, "Unix/POSIX"));
                            } else {
                                properties = properties(key);
                            }
                            object = new DefaultTemporalDatum(properties, new Date(epoch));
                        }
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Returns the temporal CRS associated to the given object, or {@code null} if none.
         */
        private static TemporalCRS crs(final IdentifiedObject object) {
            return (object instanceof TemporalCRS) ? (TemporalCRS) object : null;
        }

        /**
         * Returns the datum associated to the given object, or {@code null} if none.
         */
        private static TemporalDatum datum(final IdentifiedObject object) {
            if (object instanceof TemporalDatum) {
                return (TemporalDatum) object;
            }
            if (object instanceof TemporalCRS) {
                return ((TemporalCRS) object).getDatum();
            }
            return null;
        }
    }




    /**
     * Frequently-used engineering CRS and datum that are guaranteed to be available in SIS.
     * Below is an alphabetical list of object names available in this enumeration:
     *
     * <blockquote><table class="sis">
     *   <caption>Temporal objects accessible by enumeration constants</caption>
     *   <tr><th>Name or alias</th>    <th>Object type</th> <th>Enumeration value</th></tr>
     *   <tr><td>Computer display</td> <td>CRS, Datum</td>  <td>{@link #GEODISPLAY}</td></tr>
     *   <tr><td>Computer display</td> <td>CRS, Datum</td>  <td>{@link #DISPLAY}</td></tr>
     * </table></blockquote>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     * @since   1.1
     */
    public enum Engineering {
        /**
         * Cartesian coordinate system with (east, south) oriented axes in pixel units.
         * Axis directions are {@link AxisDirection#EAST EAST} and {@link AxisDirection#SOUTH SOUTH},
         * which implies that the coordinate system can be related to a geospatial system in some way.
         * The CRS is defined by the <cite>OGC Web Map Service Interface</cite> specification.
         *
         * <blockquote><table class="compact">
         * <caption>Computer display properties</caption>
         *   <tr><th>WMS identifier:</th> <td>CRS:1</td></tr>
         *   <tr><th>Primary names:</th>  <td>"Computer display"</td></tr>
         *   <tr><th>Direction:</th>
         *     <td>{@link AxisDirection#EAST},
         *         {@link AxisDirection#SOUTH SOUTH}</td></tr>
         *   <tr><th>Unit:</th> <td>{@link Units#PIXEL}</td></tr>
         * </table></blockquote>
         */
        GEODISPLAY(new DefaultEngineeringDatum(Map.of(
                EngineeringDatum.NAME_KEY, "Computer display",
                EngineeringDatum.ANCHOR_POINT_KEY, "Origin is in upper left."))),

        /**
         * Cartesian coordinate system with (right, down) oriented axes in pixel units.
         * This definition does not require the data to be geospatial.
         *
         * <blockquote><table class="compact">
         * <caption>Computer display properties</caption>
         *   <tr><th>Primary names:</th> <td>"Computer display"</td></tr>
         *   <tr><th>Direction:</th>
         *     <td>{@link AxisDirection#DISPLAY_RIGHT},
         *         {@link AxisDirection#DISPLAY_DOWN DISPLAY_DOWN}</td></tr>
         *   <tr><th>Unit:</th> <td>{@link Units#PIXEL}</td></tr>
         * </table></blockquote>
         */
        DISPLAY(GEODISPLAY.datum),

        /**
         * Cartesian coordinate system with (column, row) oriented axes in unity units.
         * This definition does not require the data to be geospatial.
         *
         * <blockquote><table class="compact">
         * <caption>Grid properties</caption>
         *   <tr><th>Primary names:</th> <td>"Cell indices"</td></tr>
         *   <tr><th>Direction:</th>
         *     <td>{@link AxisDirection#COLUMN_POSITIVE},
         *         {@link AxisDirection#ROW_POSITIVE ROW_POSITIVE}</td></tr>
         *   <tr><th>Unit:</th> <td>{@link Units#UNITY}</td></tr>
         * </table></blockquote>
         */
        GRID(new DefaultEngineeringDatum(singletonMap(EngineeringDatum.NAME_KEY, "Cell indices"))),

        /**
         * A single-dimensional coordinate system for time in seconds since an unknown epoch.
         * This definition can be used as a fallback when {@link Temporal} enumeration cannot be used,
         * for example because the temporal datum epoch is unknown.
         *
         * <blockquote><table class="compact">
         * <caption>Time properties</caption>
         *   <tr><th>Primary names:</th> <td>"Time"</td></tr>
         *   <tr><th>Direction:</th>
         *     <td>{@link AxisDirection#FUTURE}</td></tr>
         *   <tr><th>Unit:</th> <td>{@link Units#SECOND}</td></tr>
         * </table></blockquote>
         *
         * @see Temporal
         */
        TIME(new DefaultEngineeringDatum(singletonMap(EngineeringDatum.NAME_KEY, "Time")));

        /**
         * The datum.
         */
        private final EngineeringDatum datum;

        /**
         * The CRS, built when first needed.
         */
        private EngineeringCRS crs;

        /**
         * Creates a new enumeration value with the specified datum.
         */
        private Engineering(final EngineeringDatum datum) {
            this.datum = datum;
        }

        /**
         * Returns the coordinate reference system associated to this engineering object.
         *
         * @return the CRS associated to this enum.
         */
        public synchronized EngineeringCRS crs() {
            if (crs == null) {
                final String x, y;
                final AxisDirection dx, dy;
                final Map<String,Object> pcs = singletonMap(CartesianCS.NAME_KEY, datum.getName());
                final Map<String,Object> properties = new HashMap<>(pcs);
                CoordinateSystem cs = null;
                switch (this) {
                    case GEODISPLAY: {
                        x = "i"; dx = AxisDirection.EAST;
                        y = "j"; dy = AxisDirection.SOUTH;
                        properties.put(EngineeringCRS.NAME_KEY, new NamedIdentifier(Citations.WMS, "1"));
                        break;
                    }
                    case DISPLAY: {
                        x = "x"; dx = AxisDirection.DISPLAY_RIGHT;
                        y = "y"; dy = AxisDirection.DISPLAY_DOWN;
                        break;
                    }
                    case GRID: {
                        x = "i"; dx = AxisDirection.COLUMN_POSITIVE;
                        y = "j"; dy = AxisDirection.ROW_POSITIVE;
                        break;
                    }
                    case TIME: {
                        x  = y  = "t";
                        dx = dy = AxisDirection.FUTURE;
                        cs = new DefaultTimeCS(pcs, new DefaultCoordinateSystemAxis(
                                singletonMap(TimeCS.NAME_KEY, x), x, dx, Units.SECOND));
                        break;
                    }
                    default: throw new AssertionError(this);
                }
                if (cs == null) {
                    cs = new DefaultCartesianCS(pcs,
                            new DefaultCoordinateSystemAxis(singletonMap(CartesianCS.NAME_KEY, x), x, dx, Units.PIXEL),
                            new DefaultCoordinateSystemAxis(singletonMap(CartesianCS.NAME_KEY, y), y, dy, Units.PIXEL));
                }
                crs = new DefaultEngineeringCRS(properties, datum, cs);
            }
            return crs;
        }

        /**
         * Returns the datum associated to this engineering object.
         *
         * @return the datum associated to this enum.
         */
        public EngineeringDatum datum() {
            return datum;
        }
    }

    /**
     * Puts the name for the given key in a map of properties to be given to object constructors.
     *
     * @param  key  a constant from {@link org.apache.sis.util.resources.Vocabulary.Keys}.
     * @return the properties to give to the object constructor.
     */
    private static Map<String,?> properties(final short key) {
        return properties(Vocabulary.formatInternational(key));
    }

    /**
     * Puts the given name in a map of properties to be given to object constructors.
     */
    private static Map<String,?> properties(final InternationalString name) {
        return Map.of(NAME_KEY, new NamedIdentifier(null, name));
    }

    /**
     * Returns the same properties than the given object, except for the identifier which is set to the given code.
     */
    private static Map<String,?> properties(final IdentifiedObject template, final short code) {
        final Map<String,Object> properties = new HashMap<>(IdentifiedObjects.getProperties(template, exclude()));
        properties.put(GeographicCRS.IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, String.valueOf(code)));
        return properties;
    }

    /**
     * Properties to exclude when using another object as a template.
     */
    private static String[] exclude() {
        return new String[] {IdentifiedObject.IDENTIFIERS_KEY};
    }

    /**
     * Returns the EPSG factory to use for creating CRS, or {@code null} if none.
     * If this method returns {@code null}, then the caller will silently fallback on hard-coded values.
     */
    private static GeodeticAuthorityFactory factory() {
        if (!EPSGFactoryFallback.FORCE_HARDCODED) {
            final GeodeticAuthorityFactory factory = AuthorityFactories.EPSG();
            if (!(factory instanceof EPSGFactoryFallback)) {
                return factory;
            }
        }
        return null;
    }

    /**
     * Invoked when a factory failed to create an object.
     * After invoking this method, the caller will fallback on hard-coded values.
     */
    private static void failure(final Object caller, final String method, final FactoryException e, final int code) {
        String message = Resources.format(Resources.Keys.CanNotInstantiateGeodeticObject_1, (Constants.EPSG + ':') + code);
        message = Exceptions.formatChainedMessages(null, message, e);
        final LogRecord record = new LogRecord(Level.WARNING, message);
        if (!(e instanceof UnavailableFactoryException) || AuthorityFactories.failure((UnavailableFactoryException) e)) {
            // Append the stack trace only if the exception is the the one we expect when the factory is not available.
            record.setThrown(e);
        }
        Logging.completeAndLog(AuthorityFactories.LOGGER, caller.getClass(), method, record);
    }
}
