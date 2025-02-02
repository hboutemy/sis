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
package org.apache.sis.metadata.iso.distribution;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.internal.jaxb.gco.InternationalStringAdapter;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the distributor of and options for obtaining the resource.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Distribution}
 * {@code   └─distributionFormat………………………………………} Description of the format of the data to be distributed.
 * {@code       └─formatSpecificationCitation……} Citation/URL of the specification format.
 * {@code           ├─title……………………………………………………} Name by which the cited resource is known.
 * {@code           └─date………………………………………………………} Reference date for the cited resource.</div>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@TitleProperty(name = "description")
@XmlType(name = "MD_Distribution_Type", propOrder = {
    "description",              // New in ISO 19115-3
    "distributionFormats",
    "distributors",
    "transferOptions"
})
@XmlRootElement(name = "MD_Distribution")
public class DefaultDistribution extends ISOMetadata implements Distribution {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1331353255189686369L;

    /**
     * Brief description of a set of distribution options.
     */
    @SuppressWarnings("serial")
    private InternationalString description;

    /**
     * Provides a description of the format of the data to be distributed.
     */
    @SuppressWarnings("serial")
    private Collection<Format> distributionFormats;

    /**
     * Provides information about the distributor.
     */
    @SuppressWarnings("serial")
    private Collection<Distributor> distributors;

    /**
     * Provides information about technical means and media by which a resource is obtained
     * from the distributor.
     */
    @SuppressWarnings("serial")
    private Collection<DigitalTransferOptions> transferOptions;

    /**
     * Constructs an initially empty distribution.
     */
    public DefaultDistribution() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Distribution)
     */
    public DefaultDistribution(final Distribution object) {
        super(object);
        if (object != null) {
            distributionFormats = copyCollection(object.getDistributionFormats(), Format.class);
            distributors        = copyCollection(object.getDistributors(), Distributor.class);
            transferOptions     = copyCollection(object.getTransferOptions(), DigitalTransferOptions.class);
            if (object instanceof DefaultDistribution) {
                description = ((DefaultDistribution) object).getDescription();
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDistribution}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDistribution} instance is created using the
     *       {@linkplain #DefaultDistribution(Distribution) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDistribution castOrCopy(final Distribution object) {
        if (object == null || object instanceof DefaultDistribution) {
            return (DefaultDistribution) object;
        }
        return new DefaultDistribution(object);
    }

    /**
     * Returns a brief description of a set of distribution options.
     *
     * @return brief description of a set of distribution options.
     *
     * @since 0.5
     */
    @XmlElement(name = "description")
    @XmlJavaTypeAdapter(InternationalStringAdapter.Since2014.class)
    @UML(identifier="description", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets a brief description of a set of distribution options.
     *
     * @param  newValue  the new description.
     *
     * @since 0.5
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission(description);
        description = newValue;
    }

    /**
     * Provides a description of the format of the data to be distributed.
     *
     * @return description of the format of the data to be distributed.
     *
     * @see org.apache.sis.metadata.iso.identification.AbstractIdentification#getResourceFormats()
     */
    @Override
    @XmlElement(name = "distributionFormat")
    public Collection<Format> getDistributionFormats() {
        return distributionFormats = nonNullCollection(distributionFormats, Format.class);
    }

    /**
     * Sets a description of the format of the data to be distributed.
     *
     * @param  newValues  the new distribution formats.
     *
     * @see org.apache.sis.metadata.iso.identification.AbstractIdentification#setResourceFormats(Collection)
     */
    public void setDistributionFormats(final Collection<? extends Format> newValues) {
        distributionFormats = writeCollection(newValues, distributionFormats, Format.class);
    }

    /**
     * Provides information about the distributor.
     *
     * @return information about the distributor.
     */
    @Override
    @XmlElement(name = "distributor")
    public Collection<Distributor> getDistributors() {
        return distributors = nonNullCollection(distributors, Distributor.class);
    }

    /**
     * Sets information about the distributor.
     *
     * @param  newValues  the new distributors.
     */
    public void setDistributors(final Collection<? extends Distributor> newValues) {
        distributors = writeCollection(newValues, distributors, Distributor.class);
    }

    /**
     * Provides information about technical means and media by which a resource is obtained from the distributor.
     *
     * @return technical means and media by which a resource is obtained from the distributor.
     */
    @Override
    @XmlElement(name = "transferOptions")
    public Collection<DigitalTransferOptions> getTransferOptions() {
        return transferOptions = nonNullCollection(transferOptions, DigitalTransferOptions.class);
    }

    /**
     * Sets information about technical means and media by which a resource is obtained
     * from the distributor.
     *
     * @param  newValues  the new transfer options.
     */
    public void setTransferOptions(final Collection<? extends DigitalTransferOptions> newValues) {
        transferOptions = writeCollection(newValues, transferOptions, DigitalTransferOptions.class);
    }
}
