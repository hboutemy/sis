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
package org.apache.sis.profile.japan;

import org.apache.sis.util.Static;


/**
 * Provides implementations of Japanese extensions.
 * There is not yet public methods provided in this class.
 * Just having this module presents on the classpath is sufficient for enabling the reading of following data:
 *
 * <ul>
 *   <li>Global Change Observation Mission - Climate (GCOM-C), a.k.a. "Shikisai".</li>
 *   <li>Global Change Observation Mission - Water (GCOM-W), a.k.a. "Shizuku".</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final class JapaneseProfile extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private JapaneseProfile() {
    }
}
