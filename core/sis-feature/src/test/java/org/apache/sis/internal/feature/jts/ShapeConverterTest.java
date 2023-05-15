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
package org.apache.sis.internal.feature.jts;

import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Point;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link ShapeConverter}.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @version 1.2
 * @since   1.2
 */
public final class ShapeConverterTest extends TestCase {
    /**
     * The geometry factory used by the tests.
     */
    private final GeometryFactory factory;

    /**
     * Creates a new test case.
     */
    public ShapeConverterTest() {
        factory = new GeometryFactory();
    }

    /**
     * Verifies that the given geometry is an instance of the expected class
     * and contains the expected coordinate values.
     *
     * @param shape     the Java2D shape to convert with {@link ShapeConverter}.
     * @param type      expected class of the actual geometry.
     * @param expected  expected coordinates of the actual geometry.
     */
    private static void assertCoordinatesEqual(final Shape shape, final Class<?> type, final Coordinate... expected) {
        assertCoordinatesEqual(ShapeConverter.create(null, shape, 0.0001), type, expected);
    }

    /**
     * Verifies that the given geometry is an instance of the expected class
     * and contains the expected coordinate values.
     *
     * @param geometry  the JTS geometry to test.
     * @param type      expected class of the actual geometry.
     * @param expected  expected coordinates of the actual geometry.
     */
    private static void assertCoordinatesEqual(final Geometry geometry, final Class<?> type, final Coordinate... expected) {
        assertInstanceOf("Geometry class", type, geometry);
        assertArrayEquals("Coordinates", expected, geometry.getCoordinates());
    }

    /**
     * Tests {@link ShapeConverter} with a point.
     */
    @Test
    public void testPoint() {
        final GeneralPath shape = new GeneralPath();
        shape.moveTo(10, 20);
        assertCoordinatesEqual(shape, Point.class,
                new Coordinate(10, 20));
    }

    /**
     * Tests {@link ShapeConverter} with a line.
     */
    @Test
    public void testLine() {
        final Line2D shape = new Line2D.Double(1, 2, 3, 4);
        assertCoordinatesEqual(shape, LineString.class,
                new Coordinate(1, 2),
                new Coordinate(3, 4));
    }

    /**
     * Tests {@link ShapeConverter} with a rectangle.
     */
    @Test
    public void testRectangle() {
        final Rectangle2D shape = new Rectangle2D.Double(1, 2, 10, 20);
        assertCoordinatesEqual(shape, Polygon.class,
                new Coordinate( 1,  2),
                new Coordinate(11,  2),
                new Coordinate(11, 22),
                new Coordinate( 1, 22),
                new Coordinate( 1,  2));
    }

    /**
     * Tests {@link ShapeConverter} with a rectangle with a hole shape.
     */
    @Test
    public void testRectangleWithHole() {
        final Rectangle2D contour = new Rectangle2D.Double(1, 2, 10, 20);
        final Rectangle2D hole    = new Rectangle2D.Double(5, 6,  2,  3);
        final Area shape = new Area(contour);
        shape.subtract(new Area(hole));

        final Geometry geometry = ShapeConverter.create(factory, shape, 0.0001);
        assertInstanceOf("Geometry class", Polygon.class, geometry);
        final Polygon polygon = (Polygon) geometry;
        assertEquals(1, polygon.getNumInteriorRing());

        assertCoordinatesEqual(polygon.getExteriorRing(), LinearRing.class,
                new Coordinate(1,   2),
                new Coordinate(1,  22),
                new Coordinate(11, 22),
                new Coordinate(11,  2),
                new Coordinate(1,   2));

        assertCoordinatesEqual(polygon.getInteriorRingN(0), LinearRing.class,
                new Coordinate(7, 6),
                new Coordinate(7, 9),
                new Coordinate(5, 9),
                new Coordinate(5, 6),
                new Coordinate(7, 6));
    }

    /**
     * Tests {@link ShapeConverter} with the shape of an arbitrary text.
     * We use that as an easy way to create relatively complex shapes.
     * The arbitrary text is "Labi": 4 letters, 5 polygons (because "i" is made
     * of 2 detached polygons),* with 2 polygons ("a" and "b") having a hole.
     */
    @Test
    public void testText() {
        final Shape shape;
        final Graphics2D handler = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            final FontRenderContext fontRenderContext = handler.getFontRenderContext();
            final Font font = new Font("Monospaced", Font.PLAIN, 12);
            final GlyphVector glyphs = font.createGlyphVector(fontRenderContext, "Labi");
            shape = glyphs.getOutline();
        } finally {
            handler.dispose();
        }
        final Geometry geometry = ShapeConverter.create(factory, shape, 0.1);
        assertInstanceOf("Geometry class", MultiPolygon.class, geometry);
        final MultiPolygon mp = (MultiPolygon) geometry;
        /*
         * The "Labi" text contaons 4 characters but 'i' is split in two ploygons,
         * for a total of 5 polygons. Two letters ("a" and "b") are polyogns whith
         * hole inside them.
         */
        assertEquals(5, mp.getNumGeometries());
        for (int i=0; i<5; i++) {
            final String message = "Glyph #" + i;
            final Geometry glyph = mp.getGeometryN(i);
            assertInstanceOf(message, Polygon.class, glyph);
            assertEquals(message, (i == 1 || i == 2) ? 1 : 0,       // 'a' and 'b' should contain a hole.
                    ((Polygon) glyph).getNumInteriorRing());
        }
        /*
         * Compare the bounding boxes.
         */
        final Rectangle2D bounds2D = shape.getBounds2D();
        final Envelope env = geometry.getEnvelopeInternal();
        assertEquals(bounds2D.getMinX(), env.getMinX(), STRICT);
        assertEquals(bounds2D.getMaxX(), env.getMaxX(), STRICT);
        assertEquals(bounds2D.getMinY(), env.getMinY(), STRICT);
        assertEquals(bounds2D.getMaxY(), env.getMaxY(), STRICT);
    }
}
