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
 * Utility classes for the implementation of raster readers.
 *
 * <STRONG>Do not use!</STRONG>
 *
 * This package is for internal use by SIS only. Classes in this package
 * may change in incompatible ways in any future version without notice.
 * This package is currently in the GeoTIFF module but we may move it to
 * another module in the future for sharing with other raster formats.
 *
 * <h2>Definition of terms</h2>
 * <dl>
 *   <dt>Pixel</dt>
 *   <dd>The smallest visual component of an image. Each pixel consists of one or more sample values.
 *       For example, a pixel might have three samples storing the intensity of red, green and blue colors.</dd>
 *
 *   <dt>Sample</dt>
 *   <dd>The value of a pixel in one band. For example if an image has three bands for red, green and blue colors,
 *       then the first sample value of a pixel is the intensity of the red color.</dd>
 *
 *   <dt>Element</dt>
 *   <dd>The element in an array of primitive type for storing one or more sample value.
 *       There is usually one element per sample value, but some images pack many sample values in a single element.
 *       For example, a bilevel image stores each sample value in a single bit and packs 8 sample values per byte.</dd>
 * </dl>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
package org.apache.sis.internal.storage.inflater;
