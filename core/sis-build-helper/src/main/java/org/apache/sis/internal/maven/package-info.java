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
 * Maven plugins (others than {@link org.apache.sis.util.resources.ResourceCompilerMojo}) used
 * for building Apache SIS.
 *
 * <ul>
 *   <li>{@link org.apache.sis.internal.maven.JarCollector} collects all JAR files and their dependencies
 *     in a single {@code target/binaries} directory, using hard links instead of copying the files.</li>
 *   <li>{@link org.apache.sis.internal.maven.Assembler} builds the Apache SIS distribution file.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.3
 */
package org.apache.sis.internal.maven;
