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

/**
 * Information about the mechanisms (grid or vector) used to represent spatial data.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.spatial OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <h2>Overview</h2>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO 19115 metadata}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.spatial.AbstractSpatialRepresentation      Spatial representation} «abstract»<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.spatial.DefaultVectorSpatialRepresentation Vector spatial representation}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation   Grid spatial representation}<br>
 * {@code  │       ├─} {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeoreferenceable            Georeferenceable}<br>
 * {@code  │       └─} {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeorectified                Georectified}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.spatial.AbstractGeolocationInformation     Geolocation information} «abstract»<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGCPCollection               GCP collection}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGCP                         GCP}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.spatial.DefaultDimension                   Dimension}<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeometricObjects            Geometric objects}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.spatial.TopologyLevel             Topology level}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.spatial.GeometricObjectType       Geometric object type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.spatial.CellGeometry              Cell geometry}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.spatial.PixelOrientation          Pixel orientation}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.spatial.DimensionNameType         Dimension name type}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.spatial.SpatialRepresentationType Spatial representation type}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                 {@linkplain org.apache.sis.metadata.iso.spatial.AbstractSpatialRepresentation      Spatial representation} «abstract»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.spatial.DefaultVectorSpatialRepresentation Vector spatial representation}<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.spatial.TopologyLevel                             Topology level} «code list»<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeometricObjects            Geometric objects}<br>
 * {@code      └─} {@linkplain org.opengis.metadata.spatial.GeometricObjectType                       Geometric object type} «code list»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation   Grid spatial representation}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.spatial.DefaultDimension                   Dimension}<br>
 * {@code  │   └─} {@linkplain org.opengis.metadata.spatial.DimensionNameType                         Dimension name type} «code list»<br>
 * {@code  └─}     {@linkplain org.opengis.metadata.spatial.CellGeometry                              Cell geometry} «code list»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeoreferenceable            Georeferenceable}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.spatial.AbstractGeolocationInformation     Geolocation information} «abstract»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeorectified                Georectified}<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.spatial.PixelOrientation                          Pixel orientation} «code list»<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGCP                         GCP}<br>
 *                 {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGCPCollection               GCP collection}<br>
 *                 {@linkplain org.opengis.metadata.spatial.SpatialRepresentationType                 Spatial representation type} «code list»<br>
 * </td></tr></table>
 *
 * <h2>Null values, nil objects and collections</h2>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element cannot be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <em>live</em>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/msr/1.0/msr.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MSR,
           xmlns = {
                @XmlNs(prefix = "msr", namespaceURI = Namespaces.MSR),      // Metadata for Spatial Representation
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
           //   @XmlNs(prefix = "dqc", namespaceURI = Namespaces.DQC)       // Data Quality Common Classes
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(DQ_DataQuality.class),
    @XmlJavaTypeAdapter(DQ_Element.class),
    @XmlJavaTypeAdapter(GO_Integer.class),
    @XmlJavaTypeAdapter(GO_Record.class),
    @XmlJavaTypeAdapter(GM_Object.class),
    @XmlJavaTypeAdapter(MD_CellGeometryCode.class),
    @XmlJavaTypeAdapter(MD_Dimension.class),
    @XmlJavaTypeAdapter(MD_DimensionNameTypeCode.class),
    @XmlJavaTypeAdapter(MD_GeometricObjects.class),
    @XmlJavaTypeAdapter(MD_GeometricObjectTypeCode.class),
    @XmlJavaTypeAdapter(MD_PixelOrientationCode.class),
    @XmlJavaTypeAdapter(MD_TopologyLevelCode.class),
    @XmlJavaTypeAdapter(MI_GCP.class),
    @XmlJavaTypeAdapter(MI_GeolocationInformation.class),
    @XmlJavaTypeAdapter(RS_ReferenceSystem.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(value=GO_Boolean.class, type=boolean.class)
})
package org.apache.sis.metadata.iso.spatial;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
import org.apache.sis.internal.jaxb.geometry.GM_Object;
