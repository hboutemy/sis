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
package org.apache.sis.image;

import java.util.Map;
import java.util.stream.IntStream;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.apache.sis.internal.processing.isoline.IsolinesTest;
import org.apache.sis.test.DependsOn;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link ImageProcessor}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
@DependsOn(org.apache.sis.internal.processing.isoline.IsolinesTest.class)
public final class ImageProcessorTest extends TestCase {
    /**
     * The processor to test.
     */
    private final ImageProcessor processor;

    /**
     * Creates a new test case.
     */
    public ImageProcessorTest() {
        processor = new ImageProcessor();
    }

    /**
     * Tests {@link ImageProcessor#aggregateBands(RenderedImage...)}.
     *
     * @see BandAggregateImageTest
     */
    @Test
    public void testBandAggregate() {
        final int width  = 3;
        final int height = 4;
        final BufferedImage im1 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final BufferedImage im2 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        im1.getRaster().setSamples(0, 0, width, height, 0, IntStream.range(0, width*height).map(s -> s +  10).toArray());
        im2.getRaster().setSamples(0, 0, width, height, 0, IntStream.range(0, width*height).map(s -> s + 100).toArray());

        final Raster data = processor.aggregateBands(im1, im2).getData();
        assertEquals(new Rectangle(0, 0, width, height), data.getBounds());
        assertEquals(2, data.getNumBands());
        assertArrayEquals(
            new int[] {
                10, 100,  11, 101,  12, 102,
                13, 103,  14, 104,  15, 105,
                16, 106,  17, 107,  18, 108,
                19, 109,  20, 110,  21, 111
            },
            data.getPixels(0, 0, width, height, (int[]) null)
        );
    }

    /**
     * Tests {@link ImageProcessor#addUserProperties(RenderedImage, Map)}.
     */
    @Test
    public void testAddUserProperties() {
        final String key = "my-property";
        final String value = "my-value";
        final RenderedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_BINARY);
        final RenderedImage image  = processor.addUserProperties(source, Map.of(key, value));
        assertSame(BufferedImage.UndefinedProperty, source.getProperty(key));
        assertSame(BufferedImage.UndefinedProperty,  image.getProperty("another-property"));
        assertSame(value, image.getProperty(key));
        assertArrayEquals(new String[] {key}, image.getPropertyNames());
    }

    /**
     * Tests {@link ImageProcessor#isolines(RenderedImage, double[][], MathTransform)}.
     */
    @Test
    public void testIsolines() {
        final BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_BYTE_BINARY);
        image.getRaster().setSample(1, 1, 0, 1);
        boolean parallel = false;
        do {
            processor.setExecutionMode(parallel ? ImageProcessor.Mode.SEQUENTIAL : ImageProcessor.Mode.PARALLEL);
            final Map<Double,Shape> r = getSingleton(processor.isolines(image, new double[][] {{0.5}}, null));
            assertEquals(0.5, getSingleton(r.keySet()), STRICT);
            IsolinesTest.verifyIsolineFromMultiCells(getSingleton(r.values()));
        } while ((parallel = !parallel) == true);
    }
}
