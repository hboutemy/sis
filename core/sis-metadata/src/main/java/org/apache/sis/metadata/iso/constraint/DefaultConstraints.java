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
package org.apache.sis.metadata.iso.constraint;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.SecurityConstraints;
import org.opengis.metadata.quality.Scope;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.jaxb.metadata.MD_Releasability;
import org.apache.sis.internal.jaxb.metadata.MD_Scope;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Restrictions on the access and use of a resource or metadata.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_Constraints_Type", propOrder = {
    "useLimitations",
    "constraintApplicationScope",
    "graphic",                      // New in ISO 19115:2014
    "reference",                    // Ibid.
    "releasability",                // Ibid.
    "responsibleParty"              // Ibid.
})
@XmlRootElement(name = "MD_Constraints")
@XmlSeeAlso({
    DefaultLegalConstraints.class,
    DefaultSecurityConstraints.class
})
public class DefaultConstraints extends ISOMetadata implements Constraints {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5622398793237824161L;

    /**
     * Limitation affecting the fitness for use of the resource.
     * Example: <cite>"not to be used for navigation"</cite>.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> useLimitations;

    /**
     * Spatial and / or temporal extent and or level of the application of the constraints restrictions.
     */
    @SuppressWarnings("serial")
    private Scope constraintApplicationScope;

    /**
     * Graphic / symbol indicating the constraint.
     */
    @SuppressWarnings("serial")
    private Collection<BrowseGraphic> graphics;

    /**
     * Citation for the limitation of constraint.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> references;

    /**
     * Information concerning the parties to whom the resource can or cannot be released.
     */
    @SuppressWarnings("serial")
    private DefaultReleasability releasability;

    /**
     * Party responsible for the resource constraints.
     */
    @SuppressWarnings("serial")
    private Collection<DefaultResponsibility> responsibleParties;

    /**
     * Constructs an initially empty constraints.
     */
    public DefaultConstraints() {
    }

    /**
     * Constructs a new constraints with the given {@linkplain #getUseLimitations() use limitation}.
     *
     * @param useLimitation  the use limitation, or {@code null} if none.
     */
    public DefaultConstraints(final CharSequence useLimitation) {
        useLimitations = singleton(Types.toInternationalString(useLimitation), InternationalString.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Constraints)
     */
    public DefaultConstraints(final Constraints object) {
        super(object);
        if (object != null) {
            useLimitations             = copyCollection(object.getUseLimitations(), InternationalString.class);
            if (object instanceof DefaultConstraints) {
                final DefaultConstraints c = (DefaultConstraints) object;
                constraintApplicationScope = c.getConstraintApplicationScope();
                graphics                   = copyCollection(c.getGraphics(), BrowseGraphic.class);
                references                 = copyCollection(c.getReferences(), Citation.class);
                releasability              = c.getReleasability();
                responsibleParties         = copyCollection(c.getResponsibleParties(), DefaultResponsibility.class);
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link LegalConstraints} or
     *       {@link SecurityConstraints}, then this method delegates to the {@code castOrCopy(…)}
     *       method of the corresponding SIS subclass. Note that if the given object implements
     *       more than one of the above-cited interfaces, then the {@code castOrCopy(…)} method
     *       to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultConstraints}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultConstraints} instance is created using the
     *       {@linkplain #DefaultConstraints(Constraints) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultConstraints castOrCopy(final Constraints object) {
        if (object instanceof LegalConstraints) {
            return DefaultLegalConstraints.castOrCopy((LegalConstraints) object);
        }
        if (object instanceof SecurityConstraints) {
            return DefaultSecurityConstraints.castOrCopy((SecurityConstraints) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultConstraints) {
            return (DefaultConstraints) object;
        }
        return new DefaultConstraints(object);
    }

    /**
     * Returns the limitation affecting the fitness for use of the resource.
     * Example: <cite>"not to be used for navigation"</cite>.
     *
     * @return limitation affecting the fitness for use of the resource.
     */
    @Override
    @XmlElement(name = "useLimitation")
    public Collection<InternationalString> getUseLimitations() {
        return useLimitations = nonNullCollection(useLimitations, InternationalString.class);
    }

    /**
     * Sets the limitation affecting the fitness for use of the resource.
     * Example: <cite>"not to be used for navigation"</cite>.
     *
     * @param  newValues  the new use limitations.
     */
    public void setUseLimitations(final Collection<? extends InternationalString> newValues) {
        useLimitations = writeCollection(newValues, useLimitations, InternationalString.class);
    }

    /**
     * Returns the spatial and / or temporal extents and or levels of the application
     * of the constraints restrictions.
     *
     * @return spatial and / or temporal extents.
     *
     * @since 0.5
     */
    @XmlElement(name = "constraintApplicationScope")
    @XmlJavaTypeAdapter(MD_Scope.Since2014.class)
    @UML(identifier="constraintApplicationScope", obligation=OPTIONAL, specification=ISO_19115)
    public Scope getConstraintApplicationScope() {
        return constraintApplicationScope;
    }

    /**
     * Sets the spatial and / or temporal extents and or levels of the application of the constraints restrictions.
     *
     * @param  newValue  the new spatial and / or temporal extents.
     *
     * @since 0.5
     */
    public void setConstraintApplicationScope(final Scope newValue) {
        checkWritePermission(constraintApplicationScope);
        constraintApplicationScope = newValue;
    }

    /**
     * Returns the graphics / symbols indicating the constraint.
     *
     * @return the graphics / symbols indicating the constraint.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="graphic", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<BrowseGraphic> getGraphics() {
        return graphics = nonNullCollection(graphics, BrowseGraphic.class);
    }

    /**
     * Sets the new graphics / symbols indicating the constraint.
     *
     * @param  newValues  the new graphics / symbols indicating the constraint.
     *
     * @since 0.5
     */
    public void setGraphics(final Collection<? extends BrowseGraphic> newValues) {
        graphics = writeCollection(newValues, graphics, BrowseGraphic.class);
    }

    /**
     * Returns citations for the limitation of constraint.
     * Example: "copyright statement, license agreement, etc."
     *
     * @return citations for the limitation of constraint.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="reference", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getReferences() {
        return references = nonNullCollection(references, Citation.class);
    }

    /**
     * Sets the citations for the limitation of constraint.
     *
     * @param  newValues  the new citation for the limitation of constraint.
     *
     * @since 0.5
     */
    public void setReferences(Collection<? extends Citation> newValues) {
        references = writeCollection(newValues, references, Citation.class);
    }

    /**
     * Returns information concerning the parties to whom the resource can or cannot be released.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The return type will be changed to the {@code Releasability} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return information concerning the parties to whom the resource can or cannot be released, or {@code null} if none.
     *
     * @since 0.5
     */
    @XmlElement(name = "releasability")
    @XmlJavaTypeAdapter(MD_Releasability.Since2014.class)
    @UML(identifier="releasability", obligation=OPTIONAL, specification=ISO_19115)
    public DefaultReleasability getReleasability() {
        return releasability;
    }

    /**
     * Sets the information concerning the parties to whom the resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The argument type will be changed to the {@code Releasability} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param  newValue  the new information concerning the parties to whom the resource can or cannot be released.
     *
     * @since 0.5
     */
    public void setReleasability(final DefaultReleasability newValue) {
        checkWritePermission(releasability);
        releasability = newValue;
    }

    /**
     * Returns the parties responsible for the resource constraints.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Responsibility} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return parties responsible for the resource constraints.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="responsibleParty", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultResponsibility> getResponsibleParties() {
        return responsibleParties = nonNullCollection(responsibleParties, DefaultResponsibility.class);
    }

    /**
     * Sets the parties responsible for the resource constraints.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Responsibility} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param  newValues  the new parties responsible for the resource constraints.
     *
     * @since 0.5
     */
    public void setResponsibleParties(final Collection<? extends DefaultResponsibility> newValues) {
        responsibleParties = writeCollection(newValues, responsibleParties, DefaultResponsibility.class);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "graphic")
    private Collection<BrowseGraphic> getGraphic() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getGraphics() : null;
    }

    @XmlElement(name = "reference")
    private Collection<Citation> getReference() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getReferences() : null;
    }

    @XmlElement(name = "responsibleParty")
    private Collection<DefaultResponsibility> getResponsibleParty() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getResponsibleParties() : null;
    }
}
