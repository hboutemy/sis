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
package org.apache.sis.referencing.cs;

import java.util.Map;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests {@link DefaultCylindricalCS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 */
@DependsOn(DefaultPolarCSTest.class)
public final class DefaultCylindricalCSTest extends TestCase {
    /**
     * Tests {@link DefaultCylindricalCS#forConvention(AxesConvention)}
     * with a change from clockwise to counterclockwise axis orientation.
     */
    @Test
    public void testChangeClockwiseOrientation() {
        final DefaultCylindricalCS cs = HardCodedCS.CYLINDRICAL;
        final DefaultCylindricalCS normalized = cs.forConvention(AxesConvention.DISPLAY_ORIENTED);
        assertNotSame("Should create a new CoordinateSystem.", cs, normalized);
        assertAxisDirectionsEqual("Normalized", normalized,
                AxisDirections.AWAY_FROM,
                AxisDirections.COUNTER_CLOCKWISE,
                AxisDirection.UP);
    }

    /**
     * Tests {@link DefaultCylindricalCS#forConvention(AxesConvention)} with a change of axis order.
     * This test uses a (r) axis oriented toward South instead of "awayFrom".
     */
    @Test
    public void testChangeAxisOrder() {
        final DefaultCoordinateSystemAxis radius = HardCodedAxes.create("Radius", "r",
                AxisDirection.SOUTH, Units.METRE, 0, Double.POSITIVE_INFINITY, RangeMeaning.EXACT);

        final DefaultCylindricalCS cs = new DefaultCylindricalCS(
                Map.of(DefaultCylindricalCS.NAME_KEY, "Cylindrical"),
                HardCodedAxes.BEARING,
                HardCodedAxes.Z,
                radius);

        DefaultCylindricalCS normalized = cs.forConvention(AxesConvention.RIGHT_HANDED);
        assertNotSame("Should create a new CoordinateSystem.", cs, normalized);
        assertAxisDirectionsEqual("Right-handed", normalized,
                AxisDirections.CLOCKWISE,                       // Interchanged (r,θ) order for making right handed.
                AxisDirection.SOUTH,
                AxisDirection.UP);

        normalized = cs.forConvention(AxesConvention.NORMALIZED);
        assertNotSame("Should create a new CoordinateSystem.", cs, normalized);
        assertAxisDirectionsEqual("Normalized", normalized,
                AxisDirection.SOUTH,                            // Not modified to North because radius cannot be negative.
                AxisDirections.COUNTER_CLOCKWISE,
                AxisDirection.UP);
    }
}
