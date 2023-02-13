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

import java.util.Collection;
import org.opengis.feature.PropertyNotFoundException;

/**
 * An attributesType is a description of geometry attributes.
 * <p>
 * Based on specification :
 * <ul>
 *  <li>OGC Simple Feature Access - https://www.ogc.org/standards/sfa</li>
 *  <li>Khronos GLTF-2 - https://github.com/KhronosGroup/glTF/tree/main/specification/2.0</li>
 * </ul>
 *
 * <p>
 * Differences from OGC Simple Feature Access :<br>
 * In SFA a single attribute is possible, and exist if method isMeasured returns true.<br>
 * Transposed to the GPU model we obtain two possible attributes : POSITION(2D or 3D) and MEASURE(1D).
 *
 * <p>
 * The GPU model as defined by GLTF is more rich and allows any number of attributes.<br>
 * Each attribute may itself be composed of 1 to 16 values.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface AttributesType {

    /**
     * @param name searched attribute name
     * @return requested attribute type, never null
     * @throws PropertyNotFoundException if not found
     */
    AttributeType getAttribute(String name) throws PropertyNotFoundException;

    /**
     * Returns collection of all attributes.
     *
     * @return never null, can be empty
     */
    Collection<AttributeType> getAttributes();

}
