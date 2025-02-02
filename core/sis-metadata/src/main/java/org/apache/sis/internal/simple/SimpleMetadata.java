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
package org.apache.sis.internal.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import org.opengis.metadata.ApplicationSchemaInformation;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataExtensionInformation;
import org.opengis.metadata.PortrayalCatalogueReference;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;


/**
 * An empty implementation of ISO 19115 metadata for dataset (not for services).
 * This simple implementation presumes that the metadata describes exactly one dataset,
 * which is described by implementing methods from the {@link DataIdentification} interface.
 * The identification information itself presumes that the dataset is referenced by exactly one citation.
 *
 * <p>Unless specified otherwise, all methods in this class returns {@code null} or an empty collection by default.
 * The exceptions to this rules are the following methods:</p>
 * <ul>
 *   <li>{@code  getMetadataScopes()} returns {@code this}</li>
 *   <li>{@code  getResourceScope()} returns {@link ScopeCode#DATASET}</li>
 *   <li>{@link #getIdentificationInfo()} returns {@code this}</li>
 *   <li>{@link #getCitation()} returns {@code this}</li>
 *   <li>{@link #getSpatialRepresentationTypes()} returns {@link SpatialRepresentationType#VECTOR}</li>
 *   <li>{@link #getTopicCategories()} returns {@link TopicCategory#LOCATION}</li>
 *   <li>{@link #getPresentationForms()} returns {@link PresentationForm#TABLE_DIGITAL}</li>
 * </ul>
 *
 * Subclasses are encouraged to override the following methods (typically with hard-coded values):
 *
 * <ul>
 *   <li>{@link #getSpatialRepresentationTypes()} if the metadata describe gridded data instead of vector data.</li>
 *   <li>{@link #getTopicCategories()} if the data represent something else than locations.</li>
 *   <li>{@link #getResourceFormats()} with a hard-coded value provided by the data store implementation.</li>
 *   <li>{@link #getPresentationForms()} if the data represent something else than tabular data.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 */
public class SimpleMetadata implements Metadata, DataIdentification, Citation {
    /**
     * Creates a new metadata object.
     */
    protected SimpleMetadata() {
    }

    /**
     * Unique identifier for this metadata record.
     */
    @Override
    public String getFileIdentifier() {
        return null;
    }

    /**
     * Language(s) used for documenting metadata.
     * Also the language(s) used within the data.
     */
    @Override
    public Collection<Locale> getLanguages() {
        return Collections.emptySet();                  // We use 'Set' because we handle 'Locale' like a CodeList.
    }

    /**
     * Language(s) used for documenting metadata.
     * Also the language(s) used within the data.
     */
    @Override
    public Locale getLanguage() {
        return null;
    }

    /**
     * Language(s) used for documenting metadata.
     * Also the language(s) used within the data.
     */
    @Override
    public Collection<Locale> getLocales() {
        return Collections.emptySet();                  // We use 'Set' because we handle 'Locale' like a CodeList.
    }

    /**
     * The character coding standard used for the metadata set.
     * Also the character coding standard(s) used for the dataset.
     */
    @Override
    public Collection<CharacterSet> getCharacterSets() {
        return Collections.emptySet();                  // We use 'Set' because we handle 'Charset' like a CodeList.
    }

    /**
     * The character coding standard used for the metadata set.
     * Also the character coding standard(s) used for the dataset.
     */
    @Override
    public CharacterSet getCharacterSet() {
        return null;
    }

    /**
     * Identification of the parent metadata record.
     */
    @Override
    public String getParentIdentifier() {
        return null;
    }

    /**
     * Code for the metadata scope, fixed to {@link ScopeCode#DATASET} by default.
     * The {@code DATASET} default value is consistent with the fact that
     * {@code SimpleMetadata} implements {@link DataIdentification}.
     */
    @Override
    public Collection<ScopeCode> getHierarchyLevels() {
        return Collections.singleton(ScopeCode.DATASET);
    }

    /**
     * Description of the metadata scope.
     */
    @Override
    public Collection<String> getHierarchyLevelNames() {
        return Collections.emptySet();
    }

    /**
     * Parties responsible for the metadata information.
     */
    @Override
    public Collection<ResponsibleParty> getContacts() {
        return Collections.emptyList();
    }

    /**
     * Date(s) associated with the metadata.
     */
    @Override
    public Date getDateStamp() {
        return null;
    }

    /**
     * Citation(s) for the standard(s) to which the metadata conform.
     */
    @Override
    public String getMetadataStandardName() {
        return null;
    }

    /**
     * As of ISO 19115:2014, replaced by {@code getMetadataStandards()}
     * followed by {@link Citation#getEdition()}.
     */
    @Override
    public String getMetadataStandardVersion() {
        return null;
    }

    /**
     * Online location(s) where the metadata is available.
     */
    @Override
    public String getDataSetUri() {
        return null;
    }

    /**
     * Digital representation of spatial information in the dataset.
     */
    @Override
    public Collection<SpatialRepresentation> getSpatialRepresentationInfo() {
        return Collections.emptyList();
    }

    /**
     * Description of the spatial and temporal reference systems used in the dataset.
     */
    @Override
    public Collection<ReferenceSystem> getReferenceSystemInfo() {
        return Collections.emptyList();
    }

    /**
     * Information describing metadata extensions.
     */
    @Override
    public Collection<MetadataExtensionInformation> getMetadataExtensionInfo() {
        return Collections.emptyList();
    }

    /**
     * Basic information about the resource(s) to which the metadata applies.
     * This method returns {@code this} for allowing call to {@link #getCitation()}.
     * and other methods.
     *
     * @see #getCitation()
     * @see #getAbstract()
     * @see #getPointOfContacts()
     * @see #getSpatialRepresentationTypes()
     * @see #getSpatialResolutions()
     * @see #getTopicCategories()
     * @see #getExtents()
     * @see #getResourceFormats()
     * @see #getDescriptiveKeywords()
     */
    @Override
    public Collection<DataIdentification> getIdentificationInfo() {
        return Collections.singleton(this);
    }

    /**
     * Information about the feature and coverage characteristics.
     */
    @Override
    public Collection<ContentInformation> getContentInfo() {
        return Collections.emptyList();
    }

    /**
     * Information about the distributor of and options for obtaining the resource(s).
     */
    @Override
    public Distribution getDistributionInfo() {
        return null;
    }

    /**
     * Overall assessment of quality of a resource(s).
     */
    @Override
    public Collection<DataQuality> getDataQualityInfo() {
        return Collections.emptyList();
    }

    /**
     * Information about the catalogue of rules defined for the portrayal of a resource(s).
     */
    @Override
    public Collection<PortrayalCatalogueReference> getPortrayalCatalogueInfo() {
        return Collections.emptyList();
    }

    /**
     * Restrictions on the access and use of metadata.
     */
    @Override
    public Collection<Constraints> getMetadataConstraints() {
        return Collections.emptyList();
    }

    /**
     * Information about the conceptual schema of a dataset.
     */
    @Override
    public Collection<ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return Collections.emptyList();
    }

    /**
     * Information about the acquisition of the data.
     */
    @Override
    public Collection<AcquisitionInformation> getAcquisitionInformation() {
        return Collections.emptyList();
    }

    /**
     * Information about the frequency of metadata updates, and the scope of those updates.
     */
    @Override
    public MaintenanceInformation getMetadataMaintenance() {
        return null;
    }


    /* -------------------------------------------------------------------------------------------------
     * Implementation of the DataIdentification object returned by Metadata.getIdentificationInfo().
     * ------------------------------------------------------------------------------------------------- */

    /**
     * Citation for the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     * This method returns {@code this} for allowing call to {@link #getTitle()} and other methods.
     */
    @Override
    public Citation getCitation() {
        return this;
    }

    /**
     * Brief narrative summary of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public InternationalString getAbstract() {
        return null;
    }

    /**
     * Summary of the intentions with which the resource was developed.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public InternationalString getPurpose() {
        return null;
    }

    /**
     * Recognition of those who contributed to the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<String> getCredits() {
        return Collections.emptyList();
    }

    /**
     * Status of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Progress> getStatus() {
        return Collections.emptySet();              // We use 'Set' because 'Progress' is a CodeList.
    }

    /**
     * Identification of, and means of communication with, person(s) and organisations associated with the resource(s).
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<ResponsibleParty> getPointOfContacts() {
        return Collections.emptyList();
    }

    /**
     * Methods used to spatially represent geographic information.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     * Default implementation returns {@link SpatialRepresentationType#VECTOR}.
     * Subclasses should override this method if they represent gridded data instead of vector data.
     */
    @Override
    public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return Collections.singleton(SpatialRepresentationType.VECTOR);
    }

    /**
     * Factor which provides a general understanding of the density of spatial data in the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Resolution> getSpatialResolutions() {
        return Collections.emptyList();
    }

    /**
     * Main theme(s) of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     * Default implementation returns {@link TopicCategory#LOCATION}.
     * Subclasses should override this method if they represent other kind of data.
     */
    @Override
    public Collection<TopicCategory> getTopicCategories() {
        return Collections.singleton(TopicCategory.LOCATION);
    }

    /**
     * Spatial and temporal extent of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Extent> getExtents() {
        return Collections.emptyList();
    }

    /**
     * Information about the frequency of resource updates, and the scope of those updates.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<MaintenanceInformation> getResourceMaintenances() {
        return Collections.emptyList();
    }

    /**
     * Graphic that illustrates the resource(s) (should include a legend for the graphic).
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<BrowseGraphic> getGraphicOverviews() {
        return Collections.emptyList();
    }

    /**
     * Description of the format of the resource(s).
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Format> getResourceFormats() {
        return Collections.emptyList();
    }

    /**
     * Category keywords, their type, and reference source.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Keywords> getDescriptiveKeywords() {
        return Collections.emptyList();
    }

    /**
     * Basic information about specific application(s) for which the resource(s)
     * has/have been or is being used by different users.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Usage> getResourceSpecificUsages() {
        return Collections.emptyList();
    }

    /**
     * Information about constraints which apply to the resource(s).
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Constraints> getResourceConstraints() {
        return Collections.emptyList();
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@code getAssociatedResources()}.
     */
    @Override
    @Deprecated
    public Collection<AggregateInformation> getAggregationInfo() {
        return Collections.emptyList();
    }

    /**
     * Description of the resource in the producer's processing environment, including items
     * such as the software, the computer operating system, file name, and the dataset size.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public InternationalString getEnvironmentDescription() {
        return null;
    }

    /**
     * Any other descriptive information about the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public InternationalString getSupplementalInformation() {
        return null;
    }


    /* -------------------------------------------------------------------------------------------------
     * Implementation of the Citation object returned by DataIdentification.getCitation().
     * ------------------------------------------------------------------------------------------------- */

    /**
     * Name by which the cited resource is known.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public InternationalString getTitle() {
        return null;
    }

    /**
     * Short names or other language names by which the cited information is known.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public Collection<InternationalString> getAlternateTitles() {
        return Collections.emptyList();
    }

    /**
     * Reference dates for the cited resource.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public Collection<CitationDate> getDates() {
        return Collections.emptyList();
    }

    /**
     * Version of the cited resource.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public InternationalString getEdition() {
        return null;
    }

    /**
     * Date of the edition.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public Date getEditionDate() {
        return null;
    }

    /**
     * Unique identifier for the resource.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        return Collections.emptyList();
    }

    /**
     * Role, name, contact and position information for individuals or organisations
     * that are responsible for the resource.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public Collection<ResponsibleParty> getCitedResponsibleParties() {
        return Collections.emptyList();
    }

    /**
     * Mode in which the resource is represented.
     * This is part of the information returned by {@link #getCitation()}.
     * Default implementation returns {@link PresentationForm#TABLE_DIGITAL}.
     * Subclasses should override this method if they represent other kind of data.
     */
    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.singleton(PresentationForm.TABLE_DIGITAL);
    }

    /**
     * Information about the series, or aggregate dataset, of which the dataset is a part.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public Series getSeries() {
        return null;
    }

    /**
     * Other information required to complete the citation that is not recorded elsewhere.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public InternationalString getOtherCitationDetails() {
        return null;
    }

    /**
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Override
    @Deprecated
    public InternationalString getCollectiveTitle() {
        return null;
    }

    /**
     * International Standard Book Number.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public String getISBN() {
        return null;
    }

    /**
     * International Standard Serial Number.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public String getISSN() {
        return null;
    }
}
