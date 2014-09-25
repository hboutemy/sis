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
package org.apache.sis.metadata.iso.content;

import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.BandDefinition;
import org.opengis.metadata.content.PolarizationOrientation;
import org.opengis.metadata.content.TransferFunctionType;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.measure.ValueRange;


/**
 * Range of wavelengths in the electromagnetic spectrum.
 *
 * <p><b>Limitations:</b></p>
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
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_Band_Type", propOrder = {
    "peakResponse",
    "bitsPerValue",
    "toneGradation",
    "scaleFactor",
    "offset",
    "bandBoundaryDefinition",
    "nominalSpatialResolution",
    "transferFunctionType",
    "transmittedPolarization",
    "detectedPolarization"
})
@XmlRootElement(name = "MD_Band")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.MI_Band.class)
public class DefaultBand extends DefaultSampleDimension implements Band {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2474871120376144737L;

    /**
     * Shortest wavelength that the sensor is capable of collecting within a designated band.
     */
    private Double boundMin;

    /**
     * Longest wavelength that the sensor is capable of collecting within a designated band.
     */
    private Double boundMax;

    /**
     * Units in which sensor wavelengths are expressed.
     */
    private Unit<Length> boundUnits;

    /**
     * Designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     */
    private BandDefinition bandBoundaryDefinition;

    /**
     * Wavelength at which the response is the highest.
     */
    private Double peakResponse;

    /**
     * Number of discrete numerical values in the grid data.
     */
    private Integer toneGradation;

    /**
     * Smallest distance between which separate points can be distinguished, as specified in
     * instrument design.
     */
    private Double nominalSpatialResolution;

    /**
     * Type of transfer function to be used when scaling a physical value for a given element.
     */
    private TransferFunctionType transferFunctionType;

    /**
     * Polarization of the radiation transmitted.
     */
    private PolarizationOrientation transmittedPolarization;

    /**
     * Polarization of the radiation detected.
     */
    private PolarizationOrientation detectedPolarization;

    /**
     * Constructs an initially empty band.
     */
    public DefaultBand() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Band)
     */
    public DefaultBand(final Band object) {
        super(object);
        if (object != null) {
            boundMin                 = object.getBoundMin();
            boundMax                 = object.getBoundMax();
            boundUnits               = object.getBoundUnits();
            peakResponse             = object.getPeakResponse();
            toneGradation            = object.getToneGradation();
            bandBoundaryDefinition   = object.getBandBoundaryDefinition();
            nominalSpatialResolution = object.getNominalSpatialResolution();
            transferFunctionType     = object.getTransferFunctionType();
            transmittedPolarization  = object.getTransmittedPolarization();
            detectedPolarization     = object.getDetectedPolarization();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultBand}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultBand} instance is created using the
     *       {@linkplain #DefaultBand(Band) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultBand castOrCopy(final Band object) {
        if (object == null || object instanceof DefaultBand) {
            return (DefaultBand) object;
        }
        return new DefaultBand(object);
    }

    /**
     * Returns the shortest wavelength that the sensor is capable of collecting within a designated band.
     * The units of measurement is given by {@link #getBoundUnit()}.
     *
     * @return Shortest wavelength that the sensor is capable of collecting within a designated band,
     *         or {@code null} if unspecified.
     *
     * @since 0.5
     */
    @Override
    @ValueRange(minimum = 0)
/// @XmlElement(name = "boundMin")
    public Double getBoundMin() {
        return boundMin;
    }

    /**
     * Sets the shortest wavelength that the sensor is capable of collecting within a designated band.
     *
     * @param newValue The new shortest wavelength.
     *
     * @since 0.5
     */
    public void setBoundMin(final Double newValue) {
        checkWritePermission();
        boundMin = newValue;
    }

    /**
     * Returns the longest wavelength that the sensor is capable of collecting within a designated band.
     * The units of measurement is given by {@link #getUnits()}.
     *
     * @return Longest wavelength that the sensor is capable of collecting within a designated band,
     *         or {@code null} if unspecified.
     *
     * @since 0.5
     */
    @Override
    @ValueRange(minimum = 0)
/// @XmlElement(name = "boundMax")
    public Double getBoundMax() {
        return boundMax;
    }

    /**
     * Sets the longest wavelength that the sensor is capable of collecting within a designated band.
     *
     * @param newValue The new longest wavelength.
     *
     * @since 0.5
     */
    public void setBoundMax(final Double newValue) {
        checkWritePermission();
        boundMax = newValue;
    }

    /**
     * Returns units in which sensor wavelengths are expressed.
     *
     * @return Units in which sensor wavelengths are expressed.
     *
     * @since 0.5
     */
    @Override
/// @XmlElement(name = "boundUnits")
    public Unit<Length> getBoundUnits() {
        return boundUnits;
    }

    /**
     * Sets a new units in which sensor wavelengths are expressed.
     *
     * @param newValue the new unit.
     *
     * @since 0.5
     */
    public void setBoundUnits(final Unit<Length> newValue) {
        checkWritePermission();
        boundUnits = newValue;
    }

    /**
     * Returns the designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     *
     * @return Criterion for defining maximum and minimum wavelengths, or {@code null}.
     */
    @Override
    @XmlElement(name = "bandBoundaryDefinition", namespace = Namespaces.GMI)
    public BandDefinition getBandBoundaryDefinition() {
        return bandBoundaryDefinition;
    }

    /**
     * Sets designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     *
     * @param newValue The new band definition.
     */
    public void setBandBoundaryDefinition(final BandDefinition newValue) {
        checkWritePermission();
        bandBoundaryDefinition = newValue;
    }

    /**
     * Returns the wavelength at which the response is the highest.
     * The units of measurement is given by {@link #getUnits()}.
     *
     * @return Wavelength at which the response is the highest, or {@code null} if unspecified.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "peakResponse")
    public Double getPeakResponse() {
        return peakResponse;
    }

    /**
     * Sets the wavelength at which the response is the highest.
     *
     * @param newValue The new peak response.
     */
    public void setPeakResponse(final Double newValue) {
        checkWritePermission();
        peakResponse = newValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ValueRange(minimum = 1)
    @XmlElement(name = "bitsPerValue")
    public Integer getBitsPerValue() {
        return super.getBitsPerValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBitsPerValue(final Integer newValue) {
        super.setBitsPerValue(newValue);
    }

    /**
     * Returns the number of discrete numerical values in the grid data.
     *
     * @return Number of discrete numerical values in the grid data, or {@code null} if none.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "toneGradation")
    public Integer getToneGradation() {
        return toneGradation;
    }

    /**
     * Sets the number of discrete numerical values in the grid data.
     *
     * @param newValue The new tone gradation.
     */
    public void setToneGradation(final Integer newValue) {
        checkWritePermission();
        toneGradation = newValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @XmlElement(name = "scaleFactor")
    public Double getScaleFactor() {
        return super.getScaleFactor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setScaleFactor(final Double newValue) {
        super.setScaleFactor(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @XmlElement(name = "offset")
    public Double getOffset() {
        return super.getOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOffset(final Double newValue) {
        super.setOffset(newValue);
    }

    /**
     * Returns the smallest distance between which separate points can be distinguished,
     * as specified in instrument design.
     *
     * @return Smallest distance between which separate points can be distinguished, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0, isMinIncluded = false)
    @XmlElement(name = "nominalSpatialResolution", namespace = Namespaces.GMI)
    public Double getNominalSpatialResolution() {
        return nominalSpatialResolution;
    }

    /**
     * Sets the smallest distance between which separate points can be distinguished,
     * as specified in instrument design.
     *
     * @param newValue The new nominal spatial resolution.
     */
    public void setNominalSpatialResolution(final Double newValue) {
        checkWritePermission();
        nominalSpatialResolution = newValue;
    }

    /**
     * Returns type of transfer function to be used when scaling a physical value for a given element.
     *
     * @return Type of transfer function, or {@code null}.
     */
    @Override
    @XmlElement(name = "transferFunctionType", namespace = Namespaces.GMI)
    public TransferFunctionType getTransferFunctionType() {
        return transferFunctionType;
    }

    /**
     * Sets the type of transfer function to be used when scaling a physical value for a given element.
     *
     * @param newValue The new transfer function value.
     */
    public void setTransferFunctionType(final TransferFunctionType newValue) {
        checkWritePermission();
        transferFunctionType = newValue;
    }

    /**
     * Returns the polarization of the radiation transmitted.
     *
     * @return Polarization of the radiation transmitted, or {@code null}.
     */
    @Override
    @XmlElement(name = "transmittedPolarization", namespace = Namespaces.GMI)
    public PolarizationOrientation getTransmittedPolarization() {
        return transmittedPolarization;
    }

    /**
     * Sets the polarization of the radiation transmitted.
     *
     * @param newValue The new transmitted polarization.
     */
    public void setTransmittedPolarization(final PolarizationOrientation newValue) {
        checkWritePermission();
        transmittedPolarization = newValue;
    }

    /**
     * Returns polarization of the radiation detected.
     *
     * @return Polarization of the radiation detected, or {@code null}.
     */
    @Override
    @XmlElement(name = "detectedPolarization", namespace = Namespaces.GMI)
    public PolarizationOrientation getDetectedPolarization() {
        return detectedPolarization;
    }

    /**
     * Sets the polarization of the radiation detected.
     *
     * @param newValue The new detected polarization.
     */
    public void setDetectedPolarization(final PolarizationOrientation newValue) {
        checkWritePermission();
        detectedPolarization = newValue;
    }
}
