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
package org.apache.sis.test.visual;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.apache.sis.util.Classes;


/**
 * Base class for tests on widgets.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public abstract class Visualization {
    /**
     * The type of the object being tested.
     */
    final Class<?> testing;

    /**
     * Number of invocation of {@link #create(int)} to perform.
     */
    final int numTests;

    /**
     * Creates a new instance of {@code Visualization} which will invoke {@link #create(int)} only once.
     *
     * @param  testing  the type of object to be tested.
     */
    protected Visualization(final Class<?> testing) {
        this(testing, 1);
    }

    /**
     * Creates a new instance of {@code Visualization}.
     *
     * @param  testing   the type of object to be tested.
     * @param  numTests  number of invocation of {@link #create(int)} to perform.
     */
    protected Visualization(final Class<?> testing, final int numTests) {
        this.testing  = testing;
        this.numTests = numTests;
    }

    /**
     * Returns a title for a window created by {@link #create(int)}.
     * Default implementation returns testing class name followed by {@code index} value.
     *
     * @param  index  index of test occurrence, from 0 inclusive to the value given at construction time, exclusive.
     * @return title for the window.
     */
    protected String title(int index) {
        String title = Classes.getShortName(testing);
        if (numTests != 1) {
            title = title + " (" + index + ')';
        }
        return title;
    }

    /**
     * Creates a widget showing the object to test.
     *
     * @param  index  index of test occurrence, from 0 inclusive to the value given at construction time, exclusive.
     * @throws Exception if an error occurred while computing data for the widget.
     * @return a widget showing the object to test.
     */
    protected abstract JComponent create(int index) throws Exception;

    /**
     * Creates and shows a widget visualizing the object to test.
     */
    public final void show() {
        SwingUtilities.invokeLater(() -> DesktopPane.INSTANCE.addAndShow(this));
    }
}
