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
package org.apache.sis.internal.geometry;

import java.util.Map;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Parent interface of any geometry.
 * <p>
 * Based on specification :
 * <ul>
 *  <li>OGC Simple Feature Access - https://www.ogc.org/standards/sfa</li>
 *  <li>Khronos GLTF-2 - https://github.com/KhronosGroup/glTF/tree/main/specification/2.0</li>
 * </ul>
 *
 * <p>
 * Differences from OGC Simple Feature Access :
 * <ul>
 *  <li>SRID : replaced by getCoordinateReferenceSystem</li>
 *  <li>is3D : look at geometry CoordinateReferenceSystem instead</li>
 *  <li>isMeasured() :
 *      SFA defines only a single measure attribute attached to the geometry.
 *      Khronos/GPU geometries may defined multiple and complex attributes.
 *      Therefor a dedicated interface AttributesType is defined and accessed with {@linkplain #getAttributesType() }.</li>
 *  <li>spatial and relation methods : found on GeometryOperations.</li>
 * </ul>
 *
 * <p>
 * To be reviewed with upcoming OGC Geometry / ISO-19107.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Geometry {

    /**
     * Get geometry coordinate system.
     *
     * @return never null
     */
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Set coordinate system in which the coordinates are declared.
     * This method does not transform the coordinates.
     *
     * @param crs , not null
     * @Throws IllegalArgumentException if coordinate system is not compatible with geometrie.
     */
    void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException;

    /**
     * Get geometry attributes type.
     *
     * @return attributes type, never null
     */
    AttributesType getAttributesType();

    /**
     * Get the geometry number of dimensions.<br>
     * This is the same as coordinate system dimension.
     *
     * @return number of dimension
     */
    default int getDimension() {
        return getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Returns the name of the instantiable subtype of Geometry of which this geometric object is an instantiable member.<br>
     * The name of the subtype of Geometry is returned as a string.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return geometry subtype name.
     */
    String getGeometryType();

    /**
     * The minimum bounding box for this Geometry, returned as a Geometry.<br>
     * The polygon is defined by the corner points of the bounding box [(MINX, MINY), (MAXX, MINY), (MAXX, MAXY), (MINX, MAXY), (MINX, MINY)].<br>
     * Minimums for Z and M may be added.<br>
     * The simplest representation of an Envelope is as two direct positions, one containing all the minimums, and another all the maximums.<br>
     * In some cases, this coordinate will be outside the range of validity for the Spatial Reference System.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return Envelope in geometry coordinate reference system.
     */
    Envelope getEnvelope();

    /**
     * Exports this geometric object to a specific Well-known Text Representation of Geometry.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return this geometry in Well-known Text
     */
    String asText();

    /**
     * Exports this geometric object to a specific Well-known Binary Representation of Geometry.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return this geometry in Well-known Binary
     */
    byte[] asBinary();

    /**
     * Returns TRUE if this geometric object is the empty Geometry.
     * If true, then this geometric object represents the empty point set ∅ for the coordinate space.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return true if empty.
     */
    boolean isEmpty();

    /**
     * Returns TRUE if this geometric object has no anomalous geometric points, such as self intersection or self tangency.
     * The description of each instantiable geometric class will include the specific conditions that cause an instance
     * of that class to be classified as not simple.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return true if geometry is simple
     */
    boolean isSimple();

    /**
     * Returns the closure of the combinatorial boundary of this geometric object (Reference [1], section 3.12.2).
     * Because the result of this function is a closure, and hence topologically closed, the resulting boundary can be
     * represented using representational Geometry primitives (Reference [1], section 3.12.2).
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return boundary of the geometry
     */
    Geometry boundary();

    /**
     * Map of properties for user needs.
     * Those informations may be lost in geometry processes.
     *
     * @return Map, can be null if the geometry can not store additional informations.
     */
    Map<String,Object> userProperties();

}
