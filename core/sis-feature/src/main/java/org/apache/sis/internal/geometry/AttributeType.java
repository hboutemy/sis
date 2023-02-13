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

import org.apache.sis.internal.math.DataType;
import org.apache.sis.internal.math.SampleSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface AttributeType {

    /**
     * This attribute correspond to SFA coordinates.
     *
     * GLTF position attribute.
     * POSITION,VEC3
     * Unitless XYZ vertex positions
     */
    String ATT_POSITION = "POSITION";
    /**
     * GLTF normal attribute.
     * NORMAL,VEC3
     * Normalized XYZ vertex normals
     */
    String ATT_NORMAL = "NORMAL";
    /**
     * GLTF tangent attribute.
     * TANGENT,VEC4
     * XYZW vertex tangents where the XYZ portion is normalized,
     * and the W component is a sign value (-1 or +1) indicating handedness of the tangent basis
     */
    String ATT_TANGENT = "TANGENT";
    /**
     * GLTF indexed texture coordinate attribute.
     * TEXCOORD_n,VEC2
     * ST texture coordinates
     */
    String ATT_TEXCOORD = "TEXCOORD";
    /**
     * GLTF indexed color attribute.
     * COLOR_n,VEC3/VEC4
     * RGB or RGBA vertex color linear multiplier
     */
    String ATT_COLOR = "COLOR";
    /**
     * GLTF indexed joints attribute.
     * JOINTS_n,VEC4
     * Skinned Mesh Attribute
     */
    String ATT_JOINTS = "JOINTS";
    /**
     * GLTF indexed weights attribute.
     * JOINTS_n,VEC4
     * Skinned Mesh Attribute
     */
    String ATT_WEIGHTS = "WEIGHTS";

    /**
     * @return attribute name, not null.
     */
    String getName();

    /**
     * Returns attribute system for given name.
     *
     * @return system or null.
     */
    SampleSystem getSampleSystem();

    /**
     * Returns attribute type for given name.
     *
     * @return type or null.
     */
    DataType getDataType();
}
