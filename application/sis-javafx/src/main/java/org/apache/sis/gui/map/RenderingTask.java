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
package org.apache.sis.gui.map;

import javafx.concurrent.Task;


/**
 * Base class of tasks executed in background thread for doing rendering.
 * This is currently used only for type safety.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <V>  type of value computed by the task.
 *
 * @see MapCanvas.Renderer
 * @see MapCanvas#renderingCompleted(RenderingTask)
 *
 * @since 1.4
 */
abstract class RenderingTask<V> extends Task<V> {
    /**
     * Creates a new rendering task.
     */
    RenderingTask() {
    }
}
