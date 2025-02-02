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
package org.apache.sis.internal.netcdf;

import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Axis}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.3
 */
public final class AxisTest extends TestCase {
    /**
     * Tests {@link Axis#direction(String)}.
     */
    @Test
    public void testDirection() {
        assertSame(AxisDirection.EAST,  Axis.direction("degrees_east"));
        assertSame(AxisDirection.NORTH, Axis.direction("degrees_north"));
        assertSame(AxisDirection.EAST,  Axis.direction("degree_east"));
        assertSame(AxisDirection.NORTH, Axis.direction("degree_north"));
        assertSame(AxisDirection.EAST,  Axis.direction("degrees_E"));
        assertSame(AxisDirection.NORTH, Axis.direction("degrees_N"));
        assertSame(AxisDirection.EAST,  Axis.direction("degree_E"));
        assertSame(AxisDirection.NORTH, Axis.direction("degree_N"));
    }
}
