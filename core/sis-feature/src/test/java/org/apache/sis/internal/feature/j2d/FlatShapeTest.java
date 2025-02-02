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
package org.apache.sis.internal.feature.j2d;

import java.awt.geom.Path2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.feature.Assertions.assertPathEquals;


/**
 * Tests {@link FlatShape} subclasses and builder.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class FlatShapeTest extends TestCase {
    /**
     * Tests {@link Polyline} using {@link Path2D} as a reference implementation.
     */
    @Test
    public void testPolyline() {
        testSingle(false);
    }

    /**
     * Tests {@link Polygon} using {@link Path2D} as a reference implementation.
     */
    @Test
    public void testPolylgon() {
        testSingle(true);
    }

    /**
     * Implementation of {@link #testPolyline()} and {@link #testPolylgon()}.
     */
    private static void testSingle(final boolean closed) {
        final double[] coordinates = {
            4,5, 6,3, 8,5, -2,5, 10,4
        };
        final Polyline p = closed ? new Polygon (coordinates, coordinates.length)
                                  : new Polyline(coordinates, coordinates.length);

        final Path2D.Double r = new Path2D.Double(Path2D.WIND_NON_ZERO);
        createReferenceShape(r, coordinates, closed);
        assertPathEquals(r.getPathIterator(null), p.getPathIterator(null), STRICT);
    }

    /**
     * Appends to the given {@link Path2D} (taken as a reference implementation)
     * a polyline defined by the specified coordinates.
     *
     * @param  r            the reference implementation where to append coordinates.
     * @param  coordinates  the (x,y) tuples to append.
     * @param  closed       whether to invoke {@link Path2D#closePath()} after coordinates.
     */
    private static void createReferenceShape(final Path2D r, final double[] coordinates, final boolean closed) {
        for (int i=0; i<coordinates.length;) {
            final double x = coordinates[i++];
            final double y = coordinates[i++];
            if (i == 2) r.moveTo(x, y);
            else        r.lineTo(x, y);
        }
        if (closed) {
            r.closePath();
        }
    }

    /**
     * Tests {@link MultiPolylines}.
     */
    @Test
    public void testMultiPolylines() {
        final double[][] coordinates = {
            {4,5, 6,3, 8,5, -2,5, 10,4},
            {9,3, 7,5, -4,3},
            {3,5, 6,1, -2,7, 3,8}
        };
        final Polyline[] polylines = new Polyline[coordinates.length];
        final Path2D.Double r = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int i=0; i < polylines.length; i++) {
            polylines[i] = new Polyline(coordinates[i], coordinates[i].length);
            createReferenceShape(r, coordinates[i], false);
        }
        final MultiPolylines p = new MultiPolylines(polylines);
        assertPathEquals(r.getPathIterator(null), p.getPathIterator(null), STRICT);
    }

    /**
     * Tests {@link PathBuilder}.
     *
     * @throws TransformException never thrown in this test.
     */
    @Test
    public void testPathBuilder() throws TransformException {
        final PathBuilder b = new PathBuilder();
        b.append(new double[] {4,5, 4,5, 6,3, 8,5, -2,5, 10,4}, 8, false);      // Ignore last 2 points.
        b.append(new double[] {9,3, 7,5, 7,5, 7,5, 3,8}, 10, true);             // Add points in reverse order.
        b.append(new double[] {-2,5}, 2, false);
        b.createPolyline(true);
        b.append(new double[] {3,5, 6,1, -2,7, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                               3,8, 10,4, 6,4}, 16, false);
        b.createPolyline(true);

        final Path2D.Double r = new Path2D.Double(Path2D.WIND_NON_ZERO);
        createReferenceShape(r, new double[]{4,5, 6,3, 8,5, 3,8, 7,5, 9,3, -2,5}, true);
        createReferenceShape(r, new double[]{3,5, 6,1, -2,7}, false);
        createReferenceShape(r, new double[]{3,8, 10,4, 6,4}, true);
        assertPathEquals(r.getPathIterator(null), b.build().getPathIterator(null), STRICT);
    }
}
