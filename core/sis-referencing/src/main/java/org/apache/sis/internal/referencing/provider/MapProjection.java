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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.Unit;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;

import static org.opengis.metadata.Identifier.AUTHORITY_KEY;


/**
 * Base class for most two-dimensional map projection providers defined in this package.
 * This base class defines some descriptors for the most commonly used parameters.
 * Subclasses will declare additional parameters and group them in a
 * {@linkplain ParameterDescriptorGroup descriptor group} named {@code PARAMETERS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.6
 */
@XmlTransient
public abstract class MapProjection extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6280666068007678702L;

    /**
     * All names known to Apache SIS for the <cite>semi-major</cite> parameter.
     * This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_major"}, {@code "SemiMajorAxis"} and {@code "a"}.</p>
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> semi_major </td></tr>
     *   <tr><td> ESRI:    </td><td> Semi_Major </td></tr>
     *   <tr><td> NetCDF:  </td><td> semi_major_axis </td></tr>
     *   <tr><td> GeoTIFF: </td><td> SemiMajorAxis </td></tr>
     *   <tr><td> Proj4:   </td><td> a </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MAJOR;

    /**
     * All names known to Apache SIS for the <cite>semi-minor</cite> parameter.
     * This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_minor"}, {@code "SemiMinorAxis"} and {@code "b"}.</p>
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> semi_minor </td></tr>
     *   <tr><td> ESRI:    </td><td> Semi_Minor </td></tr>
     *   <tr><td> NetCDF:  </td><td> semi_minor_axis </td></tr>
     *   <tr><td> GeoTIFF: </td><td> SemiMinorAxis </td></tr>
     *   <tr><td> Proj4:   </td><td> b </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MINOR;

    /**
     * The ellipsoid eccentricity, computed from the semi-major and semi-minor axis lengths.
     * This a SIS-specific parameter.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> SIS:     </td><td> eccentricity </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: [0.0 … 1.0]</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final DefaultParameterDescriptor<Double> ECCENTRICITY;
    static {
        final MeasurementRange<Double> valueDomain = MeasurementRange.createGreaterThan(0, Units.METRE);
        final GenericName[] aliases = {
            new NamedIdentifier(Citations.ESRI,    "Semi_Major"),
            new NamedIdentifier(Citations.NETCDF,  "semi_major_axis"),
            new NamedIdentifier(Citations.GEOTIFF, "SemiMajorAxis"),
            new NamedIdentifier(Citations.PROJ4,   "a")
        };
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(AUTHORITY_KEY,  Citations.OGC);
        properties.put(NAME_KEY,       Constants.SEMI_MAJOR);
        properties.put(ALIAS_KEY,      aliases);
        properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(Citations.GEOTIFF, null, "2057"));
        SEMI_MAJOR = new DefaultParameterDescriptor<>(properties, 1, 1, Double.class, valueDomain, null, null);
        /*
         * Change in-place the name and aliases (we do not need to create new objects)
         * before to create the SEMI_MINOR descriptor.
         */
        properties.put(NAME_KEY, Constants.SEMI_MINOR);
        aliases[0] = new NamedIdentifier(Citations.ESRI,    "Semi_Minor");
        aliases[1] = new NamedIdentifier(Citations.NETCDF,  "semi_minor_axis");
        aliases[2] = new NamedIdentifier(Citations.GEOTIFF, "SemiMinorAxis");
        aliases[3] = new NamedIdentifier(Citations.PROJ4,   "b");
        properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(Citations.GEOTIFF, null, "2058"));
        SEMI_MINOR = new DefaultParameterDescriptor<>(properties, 1, 1, Double.class, valueDomain, null, null);
        /*
         * SIS-specific parameter for debugging purpose only.
         */
        properties.clear();
        properties.put(AUTHORITY_KEY, Citations.SIS);
        properties.put(NAME_KEY, "eccentricity");
        ECCENTRICITY = new DefaultParameterDescriptor<>(properties, 1, 1, Double.class,
                MeasurementRange.create(0d, true, 1d, true, null), null, null);
    }

    /**
     * The three-dimensional counterpart of this two-dimensional map projection.
     * This is created when first needed.
     *
     * @see #redimension(int, int)
     */
    private AbstractProvider redimensioned;

    /**
     * Constructs a math transform provider from a set of parameters. The provider
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
     *
     * @param  operationType  interface of the {@code CoordinateOperation} instances that use this projection.
     * @param  parameters     the set of parameters (never {@code null}).
     */
    protected MapProjection(final Class<? extends Projection> operationType,
                            final ParameterDescriptorGroup parameters)
    {
        super(operationType, parameters,
              EllipsoidalCS.class, true,
              CartesianCS.class,   false);
    }

    /**
     * Returns the number of source dimensions of the transforms created by this provider.
     */
    @Override
    @SuppressWarnings("deprecation")
    public final Integer getSourceDimensions() {
        return 2;
    }

    /**
     * Returns the number of target dimensions of the transforms created by this provider.
     */
    @Override
    @SuppressWarnings("deprecation")
    public final Integer getTargetDimensions() {
        return 2;
    }

    /**
     * Returns this operation method with the specified number of dimensions.
     * The number of dimensions can be only 2 or 3, and must be the same for source and target CRS.
     *
     * @return the redimensioned projection method, or {@code this} if no change is needed.
     *
     * @since 0.8
     */
    @Override
    public final AbstractProvider redimension(final int sourceDimensions, final int targetDimensions) {
        if (sourceDimensions != 3 || targetDimensions != 3) {
            return super.redimension(sourceDimensions, targetDimensions);
        } else synchronized (this) {
            if (redimensioned == null) {
                redimensioned = new MapProjection3D(this);
            }
            return redimensioned;
        }
    }

    /**
     * Validates the given parameter value. This method duplicates the verification already
     * done by {@link org.apache.sis.parameter.DefaultParameterValue#setValue(Object, Unit)}.
     * But we check again because we have no guarantee that the parameters given by the user
     * were instances of {@code DefaultParameterValue}, or that the descriptor associated to
     * the user-specified {@code ParameterValue} has sufficient information.
     *
     * @param  descriptor  the descriptor that specify the parameter to validate.
     * @param  value       the parameter value in the units given by the descriptor.
     * @throws IllegalArgumentException if the given value is out of bounds.
     *
     * @see #createZeroConstant(ParameterBuilder)
     */
    public static void validate(final ParameterDescriptor<? extends Number> descriptor, final double value)
            throws IllegalArgumentException
    {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalParameterValue_2,
                    descriptor.getName(), value));
        }
        final Comparable<? extends Number> min = descriptor.getMinimumValue();
        final Comparable<? extends Number> max = descriptor.getMaximumValue();
        final double minValue = (min instanceof Number) ? ((Number) min).doubleValue() : Double.NaN;
        final double maxValue = (max instanceof Number) ? ((Number) max).doubleValue() : Double.NaN;
        if (value < minValue || value > maxValue) {
            /*
             * RATIONAL: why we do not check the bounds if (min == max):
             * The only case when our descriptor have (min == max) is when a parameter can only be zero,
             * because of the way the map projection is defined (see e.g. Mercator1SP.LATITUDE_OF_ORIGIN).
             * But in some cases, it would be possible to deal with non-zero values, even if in principle
             * we should not. In such case we let the caller decides.
             *
             * Above check should be revisited if createZeroConstant(ParameterBuilder) is modified.
             */
            if (minValue != maxValue) {   // Compare as 'double' because we want (-0 == +0) to be true.
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                        descriptor.getName(), min, max, value));
            }
        }
    }

    /**
     * Creates a map projection from the specified group of parameter values.
     *
     * @param  factory     the factory to use for creating and concatenating the (de)normalization transforms.
     * @param  parameters  the group of parameter values.
     * @return the map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if the map projection cannot be created.
     */
    @Override
    public final MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters)
            throws ParameterNotFoundException, FactoryException
    {
        return createProjection(Parameters.castOrWrap(parameters)).createMapProjection(factory);
    }

    /**
     * Creates a map projection on an ellipsoid having a semi-major axis length of 1.
     *
     * @param  parameters  the group of parameter values.
     * @return the map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    protected abstract NormalizedProjection createProjection(final Parameters parameters) throws ParameterNotFoundException;




    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                       HELPER METHODS FOR SUBCLASSES                      ////////
    ////////                                                                          ////////
    ////////    Following methods are defined for sharing the same GenericName or     ////////
    ////////    Identifier instances when possible.                                   ////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the name of the given authority declared in the given parameter descriptor.
     * This method is used only as a way to avoid creating many instances of the same name.
     *
     * @param  authority   the authority for which to get the name.
     * @param  parameters  where to get name for the given authority.
     * @throws NoSuchElementException if the given authority has not been found.
     */
    private static GenericName sameNameAs(final Citation authority, final GeneralParameterDescriptor parameters) {
        for (final GenericName candidate : parameters.getAlias()) {
            if (candidate instanceof Identifier && ((Identifier) candidate).getAuthority() == authority) {
                return candidate;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Copies name, aliases and identifiers of the given {@code template}, except the alias and identifiers of the
     * given authority which are replaced by the alias and identifiers of the same authority in {@code replacement}.
     *
     * @param  builder      an initially clean builder where to add the names and identifiers.
     * @param  template     the parameter from which to copy names and identifiers.
     * @param  toRename     authority of the alias to rename.
     * @param  replacement  the parameter from which to get the new name for the alias to rename.
     * @return the given {@code builder}, for method call chaining.
     *
     * @since 0.8
     */
    static ParameterBuilder renameAlias(final ParameterBuilder builder, final ParameterDescriptor<Double> template,
                                        final Citation toRename, final ParameterDescriptor<Double> replacement)
    {
        renameAliases(builder, template, new Citation[] {toRename}, new ParameterDescriptor<?>[] {replacement});
        return builder;
    }

    /**
     * Same as above {@link #renameAlias(ParameterBuilder, ParameterDescriptor, Citation, ParameterDescriptor)
     * renameAlias(…)} but with two aliases to rename.
     *
     * @param  builder      an initially clean builder where to add the names and identifiers.
     * @param  template     the parameter from which to copy names and identifiers.
     * @param  s1           authority of the first alias to rename.
     * @param  r1           the parameter from which to get the new name for the first alias to rename.
     * @param  s2           authority of the second alias to rename.
     * @param  r2           the parameter from which to get the new name for the second alias to rename.
     * @return the given {@code builder}, for method call chaining.
     *
     * @since 1.1
     */
    static ParameterBuilder renameAlias(final ParameterBuilder builder, final ParameterDescriptor<Double> template,
                                        final Citation s1, final ParameterDescriptor<Double> r1,
                                        final Citation s2, final ParameterDescriptor<Double> r2)
    {
        renameAliases(builder, template, new Citation[] {s1, s2}, new ParameterDescriptor<?>[] {r1, r2});
        return builder;
    }

    /**
     * Implementation of {@code renameAlias(…)} methods.
     */
    private static void renameAliases(final ParameterBuilder builder, final ParameterDescriptor<Double> template,
            final Citation[] toRename, final ParameterDescriptor<?>[] replacement)
    {
        builder.addName(template.getName());
        final GenericName[] newNames = new GenericName[toRename.length];
        final Identifier[]  newCodes = new Identifier [toRename.length];
        for (int i=0; i<toRename.length; i++) {
            newNames[i] = sameNameAs(toRename[i], replacement[i]);
            newCodes[i] = IdentifiedObjects.getIdentifier(replacement[i], toRename[i]);
        }
        for (GenericName alias : template.getAlias()) {
            final Citation authority = ((Identifier) alias).getAuthority();
            for (int i=0; i<toRename.length; i++) {
                if (authority == toRename[i]) {
                    if (newNames[i] == null) continue;
                    alias = newNames[i];
                    newNames[i] = null;
                    break;
                }
            }
            builder.addName(alias);
        }
        for (ReferenceIdentifier id : template.getIdentifiers()) {
            final Citation authority = id.getAuthority();
            for (int i=0; i<toRename.length; i++) {
                if (authority == toRename[i]) {
                    if (newCodes[i] == null) continue;
                    id = (ReferenceIdentifier) newCodes[i];
                    newCodes[i] = null;
                    break;
                }
            }
            builder.addIdentifier(id);
        }
    }

    /**
     * Creates a remarks for parameters that are not formally EPSG parameter.
     *
     * @param  origin  the name of the projection for where the parameter is formally used.
     * @return a remarks saying that the parameter is actually defined in {@code origin}.
     */
    static InternationalString notFormalParameter(final String origin) {
        return Resources.formatInternational(Resources.Keys.NotFormalProjectionParameter_1, origin);
    }
}
