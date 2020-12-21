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
package org.apache.sis.internal.processing.image;

import java.util.Arrays;
import java.nio.DoubleBuffer;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArraysExt;


/**
 * Iterator over contouring grid cells together with an interpolator and an assembler of polyline segments.
 * A single instance of this class is created by {@code Isolines.generate(…)} for all bands to process in a
 * given image. {@code IsolineTracer} is used for doing a single iteration over all image pixels.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://en.wikipedia.org/wiki/Marching_squares">Marching squares on Wikipedia</a>
 *
 * @since 1.1
 * @module
 */
final class IsolineTracer {
    /**
     * Mask to apply on {@link Level#isDataAbove} for telling that value in a corner is higher than the level value.
     * Values are defined in {@code PixelIterator.Window} iteration order: from left to right, then top to bottom.
     *
     * <p>Note: there is some hard-coded dependencies to those exact values.
     * If values are changed, search for example for {@code log2(UPPER_RIGHT)} in comments.</p>
     */
    static final int UPPER_LEFT = 1, UPPER_RIGHT = 2, LOWER_LEFT = 4, LOWER_RIGHT = 8;

    /**
     * The 2×2 window containing pixel values in the 4 corners of current contouring grid cell.
     * Values are always stored with band index varying fastest, then column index, then row index.
     * Capacity and limit of data buffer is <var>(number of bands)</var> × 2 (width) × 2 (height).
     */
    private final DoubleBuffer window;

    /**
     * Increment to the position for reading next sample value.
     * It corresponds to the number of bands in {@link #window}.
     */
    private final int pixelStride;

    /**
     * Pixel coordinate on the left side of the cell where to interpolate.
     */
    int x;

    /**
     * Pixel coordinate on the top side of the cell where to interpolate.
     */
    int y;

    /**
     * Threshold for considering two coordinates as equal.
     * Shall be a value between 0 and 0.5.
     */
    private final double tolerance;

    /**
     * Final transform to apply on coordinates.
     */
    private final MathTransform gridToCRS;

    /**
     * Creates a new position for the given data window.
     */
    IsolineTracer(final DoubleBuffer window, final int pixelStride, double tolerance, final MathTransform gridToCRS) {
        this.window      = window;
        this.pixelStride = pixelStride;
        this.tolerance   = (tolerance = Math.min(Math.abs(tolerance), 0.5)) >= 0 ? tolerance : 0;
        this.gridToCRS   = gridToCRS;
    }

    /**
     * Builder of polylines for a single level. The segments to create are determined by a set
     * of {@linkplain #isDataAbove four flags} (one for each corner) encoded in an integer.
     * The meaning of those flags is described in Wikipedia "Marching squares" article,
     * except that this implementation uses different values.
     */
    final class Level {
        /**
         * The level value. This is a copy of {@link Isolines#levelValues} at the index of this level.
         *
         * @see #interpolate(int, int)
         */
        final double value;

        /**
         * Bitset telling which corners have a value greater than this isoline level {@linkplain #value}.
         * Each corner is associated to one of the bits illustrated below, where bit (0) is the less significant.
         * Note that this bit order is different than the order used in Wikipedia "Marching squares" article.
         * The order used in this class allows more direct bitwise operations as described in next section.
         *
         * {@preformat text
         *     (0)╌╌╌(1)
         *      ╎     ╎
         *     (2)╌╌╌(3)
         * }
         *
         * Bits are set to 1 where the data value is above the isoline {@linkplain #value}, and 0 where the data
         * value is equal or below the isoline value. Data values exactly equal to the isoline value are handled
         * as if they were greater. It does not matter for interpolations: we could flip this convention randomly,
         * the interpolated points would still the same. It could change the way line segments are assembled in a
         * single {@link Polyline}, but the algorithm stay consistent if we always apply the same rule for all points.
         *
         * <h4>Reusing bits from previous iteration</h4>
         * We will iterate on pixels from left to right, then from top to bottom. With that iteration order,
         * bits 0 and 2 can be obtained from the bit pattern of previous iteration with a simple bit shift.
         *
         * @see #UPPER_LEFT
         * @see #UPPER_RIGHT
         * @see #LOWER_LEFT
         * @see #LOWER_RIGHT
         */
        int isDataAbove;

        /**
         * The polyline to be continued on the next column. This is a single instance because iteration happens
         * from left to right before top to bottom. This instance is non-empty if the cell in previous iteration
         * was like below (all those examples have a line crossing the right border):
         *
         * {@preformat text
         *     ●╌╌╌╌╌╌●              ○╌╱╌╌╌╌●╱             ○╌╌╌╌╲╌●
         *     ╎      ╎              ╎╱     ╱              ╎     ╲╎
         *    ─┼──────┼─             ╱     ╱╎              ╎      ╲
         *     ○╌╌╌╌╌╌○             ╱●╌╌╌╌╱╌○              ○╌╌╌╌╌╌○╲
         * }
         *
         * This field {@linkplain Polyline#isEmpty() is empty} if the cell in previous iteration was like below
         * (no line cross the right border):
         *
         * {@preformat text
         *     ○╌╲╌╌╌╌●              ○╌╌╌┼╌╌●
         *     ╎  ╲   ╎              ╎   │  ╎
         *     ╎   ╲  ╎              ╎   │  ╎
         *     ○╌╌╌╌╲╌●              ○╌╌╌┼╌╌●
         * }
         */
        private final Polyline polylineOnLeft;

        /**
         * The polylines in each column which need to be continued on the next row.
         * This array contains empty instances in columns where there is no polyline to continue on next row.
         * For non-empty element at index <var>x</var>, values on the left border are given by pixels at coordinate
         * {@code x} and values on the right border are given by pixels at coordinate {@code x+1}. Example:
         *
         * {@preformat text
         *            ○╌╌╌╌╌╌●╱
         *            ╎ Top  ╱
         *            ╎ [x] ╱╎
         *     ●╌╌╌╌╌╌●╌╌╌╌╱╌○
         *     ╎ Left ╎██████╎ ← Cell where to create a segment
         *    ─┼──────┼██████╎
         *     ○╌╌╌╌╌╌○╌╌╌╌╌╌○
         *            ↑
         *     x coordinate of first pixel (upper-left corner)
         * }
         */
        private final Polyline[] polylinesOnTop;

        /**
         * The isolines as a Java2D shape, created when first needed. The {@link Polyline} coordinates are copied
         * in this path when a geometry is closed. This is the shape to be returned to user for this level after
         * we finished to process all cells.
         *
         * @see #writeTo(Path2D, Polyline...)
         */
        Path2D path;

        /**
         * Creates new isoline levels for the given value.
         *
         * @param  value  the isoline level value.
         * @param  width  the contouring grid cell width (one cell smaller than image width).
         */
        Level(final double value, final int width) {
            this.value = value;
            polylineOnLeft = new Polyline();
            polylinesOnTop = new Polyline[width];
            for (int i=0; i<width; i++) {
                polylinesOnTop[i] = new Polyline();
            }
        }

        /**
         * Initializes the {@link #isDataAbove} value with values for the column on the right side.
         * After this method call, the {@link #UPPER_RIGHT} and {@link #LOWER_RIGHT} bits still need to be set.
         */
        final void nextColumn() {
            /*
             * Move bits on the right side to the left side.
             * The 1 operand in >>> is the hard-coded value
             * of    log2(UPPER_RIGHT) - log2(UPPER_LEFT)
             * and   log2(LOWER_RIGHT) - log2(LOWER_LEFT).
             */
            isDataAbove = (isDataAbove & (UPPER_RIGHT | LOWER_RIGHT)) >>> 1;
        }

        /**
         * Adds segments computed for values in a single pixel. Interpolations are determined by the 4 lowest bits
         * of {@link #isDataAbove}. The {@link #polylineOnLeft} and {@code polylinesOnTop[x]} elements are updated
         * by this method.
         *
         * <h4>How NaN values are handled</h4>
         * This algorithm does not need special attention for {@link Double#NaN} values. Interpolations will produce
         * {@code NaN} values and append them to the correct polyline (which does not depend on interpolation result)
         * like real values. Those NaN values will be filtered later in another method, when copying coordinates in
         * {@link Path2D} objects.
         */
        @SuppressWarnings("AssertWithSideEffects")
        final void interpolate() throws TransformException {
            switch (isDataAbove) {
                default: {
                    throw new AssertionError(isDataAbove);      // Should never happen.
                }
                /*     ○╌╌╌╌╌╌○        ●╌╌╌╌╌╌●
                 *     ╎      ╎        ╎      ╎
                 *     ╎      ╎        ╎      ╎
                 *     ○╌╌╌╌╌╌○        ●╌╌╌╌╌╌●
                 */
                case 0:
                case UPPER_LEFT | UPPER_RIGHT | LOWER_LEFT | LOWER_RIGHT: {
                    assert polylinesOnTop[x].isEmpty();
                    assert polylineOnLeft   .isEmpty();
                    break;
                }
                /*     ○╌╌╌╌╌╌○        ●╌╌╌╌╌╌●
                 *    ─┼──────┼─      ─┼──────┼─
                 *     ╎      ╎        ╎      ╎
                 *     ●╌╌╌╌╌╌●        ○╌╌╌╌╌╌○
                 */
                case LOWER_LEFT | LOWER_RIGHT:
                case UPPER_LEFT | UPPER_RIGHT: {
                    assert polylinesOnTop[x].isEmpty();
                    interpolateMissingLeftSide();
                    interpolateOnRightSide();                   // Will be the left side of next column.
                    break;
                }
                /*     ○╌╌╌┼╌╌●        ●╌╌╌┼╌╌○
                 *     ╎   │  ╎        ╎   │  ╎
                 *     ╎   │  ╎        ╎   │  ╎
                 *     ○╌╌╌┼╌╌●        ●╌╌╌┼╌╌○
                 */
                case UPPER_RIGHT | LOWER_RIGHT:
                case UPPER_LEFT  | LOWER_LEFT: {
                    assert polylineOnLeft.isEmpty();
                    final Polyline polylineOnTop = polylinesOnTop[x];
                    interpolateMissingTopSide(polylineOnTop);
                    interpolateOnBottomSide(polylineOnTop);     // Will be top side of next row.
                    break;
                }
                /*    ╲○╌╌╌╌╌╌○       ╲●╌╌╌╌╌╌●
                 *     ╲      ╎        ╲      ╎
                 *     ╎╲     ╎        ╎╲     ╎
                 *     ●╌╲╌╌╌╌○        ○╌╲╌╌╌╌●
                 */
                case LOWER_LEFT:
                case UPPER_LEFT | UPPER_RIGHT | LOWER_RIGHT: {
                    assert polylinesOnTop[x].isEmpty();
                    interpolateMissingLeftSide();
                    interpolateOnBottomSide(polylinesOnTop[x].transferFrom(polylineOnLeft));
                    break;
                }
                /*     ○╌╌╌╌╲╌●        ●╌╌╌╌╲╌○
                 *     ╎     ╲╎        ╎     ╲╎
                 *     ╎      ╲        ╎      ╲
                 *     ○╌╌╌╌╌╌○╲       ●╌╌╌╌╌╌●╲
                 */
                case UPPER_RIGHT:
                case UPPER_LEFT | LOWER_LEFT | LOWER_RIGHT: {
                    assert polylineOnLeft.isEmpty();
                    interpolateMissingTopSide(polylineOnLeft.transferFrom(polylinesOnTop[x]));
                    interpolateOnRightSide();
                    break;
                }
                /*     ○╌╌╌╌╌╌○╱       ●╌╌╌╌╌╌●╱
                 *     ╎      ╱        ╎      ╱
                 *     ╎     ╱╎        ╎     ╱╎
                 *     ○╌╌╌╌╱╌●        ●╌╌╌╌╱╌○
                 */
                case LOWER_RIGHT:
                case UPPER_LEFT | UPPER_RIGHT | LOWER_LEFT: {
                    assert polylinesOnTop[x].isEmpty();
                    assert polylineOnLeft   .isEmpty();
                    interpolateOnRightSide();
                    interpolateOnBottomSide(polylinesOnTop[x].attach(polylineOnLeft));
                    // Bottom of this cell will be top of next row.
                    break;
                }
                /*     ●╌╱╌╌╌╌○        ○╌╱╌╌╌╌●
                 *     ╎╱     ╎        ╎╱     ╎
                 *     ╱      ╎        ╱      ╎
                 *    ╱○╌╌╌╌╌╌○       ╱●╌╌╌╌╌╌●
                 */
                case UPPER_LEFT:
                case UPPER_RIGHT | LOWER_LEFT | LOWER_RIGHT: {
                    interpolateMissingLeftSide();
                    interpolateOnTopSide(polylineOnLeft);
                    closeLeftWithTop(polylinesOnTop[x]);
                    break;
                }
                /*     ○╌╱╌╌╌╌●╱      ╲●╌╌╌╌╲╌○
                 *     ╎╱     ╱        ╲     ╲╎
                 *     ╱     ╱╎        ╎╲     ╲
                 *    ╱●╌╌╌╌╱╌○        ○╌╲╌╌╌╌●╲
                 *
                 * Disambiguation of saddle points: use the average data value for the center of the cell.
                 * If the estimated center value is greater than the isoline value, the above drawings are
                 * okay and we do not need to change `isDataAbove`. This is the left side illustrated below.
                 * But if the center value is below isoline value, then we need to flip `isDataAbove` bits
                 * (conceptually; not really because we need to keep `isDataAbove` value for next iteration).
                 * This is the right side illustrated below.
                 *
                 *     ○╱╌╌●╱      ╲●╌╌╲○                        ╲○╌╌╲●        ●╱╌╌○╱
                 *     ╱ ● ╱        ╲ ● ╲                         ╲ ○ ╲        ╱ ○ ╱
                 *    ╱●╌╌╱○        ○╲╌╌●╲                        ●╲╌╌○╲      ╱○╌╌╱●
                 */
                case UPPER_RIGHT | LOWER_LEFT:
                case UPPER_LEFT | LOWER_RIGHT: {
                    double average = 0;
                    {   // Compute sum of 4 corners.
                        final DoubleBuffer data = window;
                        final int limit = data.limit();
                        int p = data.position();
                        do average += data.get(p);
                        while ((p += pixelStride) < limit);
                        assert (p -= data.position()) == pixelStride * 4 : p;
                        average /= 4;
                    }
                    boolean LLtoUR = isDataAbove == (LOWER_LEFT | UPPER_RIGHT);
                    LLtoUR ^= (average <= value);
                    interpolateMissingLeftSide();
                    final Polyline polylineOnTop = polylinesOnTop[x];
                    if (LLtoUR) {
                        interpolateOnTopSide(polylineOnLeft);
                        closeLeftWithTop(polylineOnTop);
                        interpolateOnRightSide();
                        interpolateOnBottomSide(polylineOnTop.attach(polylineOnLeft));
                    } else {
                        final Polyline swap = new Polyline().transferFrom(polylineOnTop);
                        interpolateOnBottomSide(polylineOnTop.transferFrom(polylineOnLeft));
                        interpolateMissingTopSide(polylineOnLeft.transferFrom(swap));
                        interpolateOnRightSide();
                    }
                    break;
                }
            }
        }

        /**
         * Appends to {@link #polylineOnLeft} a point interpolated on the left side if that point is missing.
         * This interpolation should happens only in the first column.
         */
        private void interpolateMissingLeftSide() {
            if (polylineOnLeft.size == 0) {
                polylineOnLeft.append(x, y + interpolate(0, 2*pixelStride));
            }
        }

        /**
         * Appends to {@code polylineOnTop} a point interpolated on the top side if that point is missing.
         * This interpolation should happens only in the first row.
         */
        private void interpolateMissingTopSide(final Polyline polylineOnTop) {
            if (polylineOnTop.size == 0) {
                interpolateOnTopSide(polylineOnTop);
            }
        }

        /**
         * Appends to the given polyline a point interpolated on the top side.
         */
        private void interpolateOnTopSide(final Polyline appendTo) {
            appendTo.append(x + interpolate(0, pixelStride), y);
        }

        /**
         * Appends to {@link #polylineOnLeft} a point interpolated on the right side.
         * The polyline on right side will become {@code polylineOnLeft} in next column.
         */
        private void interpolateOnRightSide() {
            polylineOnLeft.append(x + 1, y + interpolate(pixelStride, 3*pixelStride));
        }

        /**
         * Appends to the given polyline a point interpolated on the bottom side.
         * The polyline on top side will become a {@code polylineOnBottoù} in next row.
         */
        private void interpolateOnBottomSide(final Polyline polylineOnTop) {
            polylineOnTop.append(x + interpolate(2*pixelStride, 3*pixelStride), y + 1);
        }

        /**
         * Interpolates the position where the isoline passes between two values.
         * The {@link #window} buffer position shall be the first sample value
         * for the band to process.
         *
         * @param  i1  index of first value in the buffer, ignoring band offset.
         * @param  i2  index of second value in the buffer, ignoring band offset.
         * @return a value interpolated between the values at the two given indices.
         */
        private double interpolate(final int i1, final int i2) {
            final DoubleBuffer data = window;
            final int    p  = data.position();
            final double v1 = data.get(p + i1);
            final double v2 = data.get(p + i2);
            return (value - v1) / (v2 - v1);
        }

        /**
         * Joins {@link #polylineOnLeft} with {@code polylineOnTop}, then writes to {@link #path} if the result
         * is a closed polygon. The two polylines (left and top) will become empty after this method call.
         */
        private void closeLeftWithTop(final Polyline polylineOnTop) throws TransformException {
            if (polylineOnLeft.opposite == polylineOnTop) {
                // The polygon can be closed.
                path = writeTo(path, polylineOnTop, polylineOnLeft);
            } else {
                path = writeTo(path, polylineOnLeft.opposite, polylineOnLeft, polylineOnTop, polylineOnTop.opposite);
            }
        }

        /**
         * Writes the content of given polyline.
         * The given polyline will become empty after this method call.
         */
        private void writeUnclosed(final Polyline polyline) throws TransformException {
            path = writeTo(path, polyline.opposite, polyline);
        }

        /**
         * Invoked after iteration on a single row has been completed. If there is a polyline
         * finishing on the right image border, that polyline needs to be written now because
         * it will not be continued by cells on next rows.
         */
        final void finishedRow() throws TransformException {
            if (!polylineOnLeft.transferToOpposite()) {
                writeUnclosed(polylineOnLeft);
            }
            isDataAbove = 0;
        }

        /**
         * Invoked after the iteration has been completed on the full image.
         * This method flushes all reminding polylines to the {@link #path}.
         * It assumes that {@link #finishedRow()} has already been invoked.
         */
        final void finish() throws TransformException {
            for (int i=0; i < polylinesOnTop.length; i++) {
                writeUnclosed(polylinesOnTop[i]);
                polylinesOnTop[i] = null;
            }
        }
    }

    /**
     * Coordinates of a polyline under construction. Coordinates can be appended in only one direction.
     * If the polyline may growth on both directions (which happens if the polyline crosses the bottom
     * side and the right side of a cell), then the two directions are handled by two distinct instances
     * connected by their {@link #opposite} field.
     *
     * <p>When a polyline has been completed, its content is copied to {@link Level#path}
     * and the {@code Polyline} object is recycled for a new polyline.</p>
     */
    private static final class Polyline {
        /**
         * Number of coordinates in a tuple.
         */
        static final int DIMENSION = 2;

        /**
         * Coordinates as (x,y) tuples. This array is expanded as needed.
         */
        double[] coordinates;

        /**
         * Number of valid elements in the {@link #coordinates} array.
         * This is twice the number of points.
         */
        int size;

        /**
         * If the polyline has points added to its two extremities, the other extremity. Otherwise {@code null}.
         * The first point of {@code opposite} polyline is connected to the first point of this polyline.
         * Consequently when those two polylines are joined in a single polyline, the coordinates of either
         * {@code this} or {@code opposite} must be iterated in reverse order.
         */
        Polyline opposite;

        /**
         * Creates an initially empty polyline.
         */
        Polyline() {
            coordinates = ArraysExt.EMPTY_DOUBLE;
        }

        /**
         * Discards all coordinates in this polyline.
         */
        final void clear() {
            opposite = null;
            size = 0;
        }

        /**
         * Returns whether this polyline is empty.
         */
        final boolean isEmpty() {
            return size == 0 & (opposite == null);
        }

        /**
         * Declares that the specified polyline will add points in the direction opposite to this polyline.
         * This happens when the polyline crosses the bottom side and the right side of a cell (assuming an
         * iteration from left to right and top to bottom).
         *
         * @return {@code this} for method calls chaining.
         */
        final Polyline attach(final Polyline other) {
            assert (opposite == null) & (other.opposite == null);
            other.opposite = this;
            opposite = other;
            return this;
        }

        /**
         * Transfers all coordinates from given polylines to this polylines, in same order.
         * This is used when polyline on the left side continues on bottom side,
         * or conversely when polyline on the top side continues on right side.
         * This polyline shall be empty before this method is invoked.
         * The given source will become empty after this method returned.
         *
         * @param  source  the source from which to take data.
         * @return {@code this} for method calls chaining.
         */
        final Polyline transferFrom(final Polyline source) {
            assert isEmpty();
            final double[] swap = coordinates;
            coordinates = source.coordinates;
            size        = source.size;
            opposite    = source.opposite;
            if (opposite != null) {
                opposite.opposite = this;
            }
            source.clear();
            source.coordinates = swap;
            return this;
        }

        /**
         * Transfers all coordinates from this polyline to the polyline going in opposite direction.
         * This is used when this polyline reached the right image border, in which case its data
         * will be lost if we do not copy them somewhere.
         *
         * @return {@code true} if coordinates have been transferred,
         *         or {@code false} if there is no opposite direction.
         */
        final boolean transferToOpposite() {
            if (opposite == null) {
                return false;
            }
            final int sum = size + opposite.size;
            double[] data = opposite.coordinates;
            if (sum > data.length) {
                data = new double[sum];
            }
            System.arraycopy(opposite.coordinates, 0, data, size, opposite.size);
            for (int i=0, t=size; (t -= DIMENSION) >= 0;) {
                data[t  ] = coordinates[i++];
                data[t+1] = coordinates[i++];
            }
            opposite.size = sum;
            opposite.coordinates = data;
            opposite.opposite = null;
            clear();
            return true;
        }

        /**
         * Appends given coordinates to this polyline.
         *
         * @param  x  first coordinate of the (x,y) tuple to add.
         * @param  y  second coordinate of the (x,y) tuple to add.
         */
        final void append(final double x, final double y) {
            if (size >= coordinates.length) {
                coordinates = Arrays.copyOf(coordinates, Math.max(Math.multiplyExact(size, 2), 32));
            }
            coordinates[size++] = x;
            coordinates[size++] = y;
        }

        /**
         * Returns a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder(30).append('[');
            if (size >= DIMENSION) {
                b.append((float) coordinates[0]).append(", ").append((float) coordinates[1]);
                final int n = size - DIMENSION;
                if (n >= DIMENSION) {
                    b.append(", ");
                    if (size >= DIMENSION*3) {
                        b.append(" … (").append(size / DIMENSION - 2).append(" pts) … ");
                    }
                    b.append((float) coordinates[n]).append(", ").append((float) coordinates[n+1]);
                }
            }
            return b.append(']').toString();
        }
    }

    /**
     * If the shape delimited by given polylines has a part with zero width or height ({@literal i.e.} a spike),
     * truncates the polylines for removing that spike. This situation happens when some pixel values are exactly
     * equal to isoline value, as in the picture below:
     *
     * {@preformat text
     *     ●╌╌╌╲╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○
     *     ╎    ╲ ╎      ╎      ╎      ╎
     *     ╎     ╲╎      ╎   →  ╎      ╎
     *     ●╌╌╌╌╌╌●──────●──────●⤸╌╌╌╌╌○
     *     ╎     ╱╎      ╎   ←  ╎      ╎
     *     ╎    ╱ ╎      ╎      ╎      ╎
     *     ●╌╌╌╱╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○
     * }
     *
     * The spike may appear or not depending on the convention adopted for strictly equal values.
     * In above picture, the spike appears because the convention used in this implementation is:
     *
     * <ul>
     *   <li>○: {@literal pixel value < isoline value}.</li>
     *   <li>●: {@literal pixel value ≥ isoline value}.</li>
     * </ul>
     *
     * If the following convention was used instead, the spike would not appear in above figure
     * (but would appear in different situations):
     *
     * <ul>
     *   <li>○: {@literal pixel value ≤ isoline value}.</li>
     *   <li>●: {@literal pixel value > isoline value}.</li>
     * </ul>
     *
     * This method detects and removes those spikes for avoiding convention-dependent results.
     * We assume that spikes can appear only at the junction between two {@link Polyline} instances.
     * Rational: having a spike require that we move forward then backward on the same coordinates,
     * which is possible only with a non-null {@link Polyline#opposite} field.
     *
     * @param  p0       first polyline, or {@code null}.
     * @param  p1       second polyline, or {@code null}.
     * @param  reverse  whether points in {@code p0} shall be read in reverse order.
     *                  If {@code true},  (p0, p1) are read in (reverse, forward) point order.
     *                  If {@code false}, (p0, p1) are read in (forward, reverse) point order.
     */
    private static void removeSpikes(final Polyline p0, final Polyline p1, final boolean reverse) {
        if (p0 == null || p1 == null || p0.size == 0 || p1.size == 0) {
            return;
        }
        double xo = Double.NaN;     // First point.
        double yo = Double.NaN;
        int equalityMask = 0;       // Bit 1 and 2 set when x and y values (respectively) are equal.
        int spike0 = 0;             // Index where both coordinates become different than (xo,yo).
        int spike1;
        for (boolean first = true;;) {              // Executed exactly 2 times: for p0 and for p1.
            final Polyline p = first ? p0 : p1;
            final double[] coordinates = p.coordinates;
            final int size = p.size;
            spike1 = 0;
            do {
                final double x, y;
                if (reverse == first) {
                    y = coordinates[size - ++spike1];
                    x = coordinates[size - ++spike1];
                } else {
                    x = coordinates[spike1++];
                    y = coordinates[spike1++];
                }
                if (equalityMask == 0) {            // This condition is true only for the first point.
                    equalityMask = 3;
                    xo = x;
                    yo = y;
                } else {
                    final int before = equalityMask;
                    if (x != xo) equalityMask &= ~1;
                    if (y != yo) equalityMask &= ~2;
                    if (equalityMask == 0) {
                        equalityMask = before;              // For comparison of next polyline.
                        spike1 -= Polyline.DIMENSION;       // Restore previous position.
                        if (spike1 == 0) return;
                        break;
                    }
                }
            } while (spike1 < size);
            /*
             * Here we found a point which is not on the spike,
             * or we finished examining all points on a polyline.
             */
            if (first) {
                first  = false;
                spike0 = spike1;
            } else {
                break;
            }
        }
        /*
         * Here we have range of indices where the polygon has a width or height of zero.
         * Search for a common point, then truncate at that point. If `reverse` is false,
         * then the two polylines are attached as below and the spike to remove is at the
         * end of both polylines:
         *
         *     0       p0.size|p1.size       0
         *     ●──●──●──●──●──●──●──●──●──●──●
         *              └ remove ┘
         *
         * If `reverse` is true, then the two polylines are attached as below and the spike
         * to remove is at the beginning of both polylines:
         *
         *     p0.size        0        p1.size
         *     ●──●──●──●──●──●──●──●──●──●──●
         *              └ remove ┘
         */
        int i0 = spike0;
        do {
            if (reverse) {
                yo = p0.coordinates[--i0];
                xo = p0.coordinates[--i0];
            } else {
                xo = p0.coordinates[p0.size - i0--];
                yo = p0.coordinates[p0.size - i0--];
            }
            int i1 = spike1;
            do {
                final double x, y;
                if (reverse) {
                    y = p1.coordinates[--i1];
                    x = p1.coordinates[--i1];
                } else {
                    x = p1.coordinates[p1.size - i1--];
                    y = p1.coordinates[p1.size - i1--];
                }
                if (x == xo && y == yo) {
                    p0.size -= i0;
                    p1.size -= i1;
                    if (reverse) {
                        System.arraycopy(p0.coordinates, i0, p0.coordinates, 0, p0.size);
                        System.arraycopy(p1.coordinates, i1, p1.coordinates, 0, p1.size);
                    }
                    return;
                }
            } while (i1 > 0);
        } while (i0 > 0);
    }

    /**
     * Writes all given polylines to the specified path. Null {@code Polyline} instances are ignored.
     * {@code Polyline} instances at even index are written with their coordinates in reverse order.
     * All given polylines are cleared by this method.
     *
     * @param  path       where to write the polylines, or {@code null} if not yet created.
     * @param  polylines  the polylines to write.
     * @return the given path, or a newly created path if the argument was null.
     */
    private Path2D writeTo(Path2D path, final Polyline... polylines) throws TransformException {
        for (int i=1; i<polylines.length; i++) {
            removeSpikes(polylines[i-1], polylines[i], (i & 1) != 0);
        }
        double xo = Double.NaN;     // First point of current polygon.
        double yo = Double.NaN;
        double px = Double.NaN;     // Previous point.
        double py = Double.NaN;
        int state = PathIterator.SEG_MOVETO;
        for (int pi=0; pi < polylines.length; pi++) {
            final Polyline p = polylines[pi];
            if (p == null) {
                continue;
            }
            final int size = p.size;
            if (size == 0) {
                assert p.isEmpty();
                continue;
            }
            final boolean  reverse     = (pi & 1) == 0;
            final double[] coordinates = p.coordinates;
            gridToCRS.transform(coordinates, 0, coordinates, 0, size / Polyline.DIMENSION);
            int i = 0;
            do {
                final double x, y;
                if (reverse) {
                    y = coordinates[size - ++i];
                    x = coordinates[size - ++i];
                } else {
                    x = coordinates[i++];
                    y = coordinates[i++];
                }
                if (!(Math.abs(x - px) <= tolerance && Math.abs(y - py) <= tolerance)) {
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        state = PathIterator.SEG_MOVETO;        // Next point will be in a separated polygon.
                    } else switch (state) {
                        case PathIterator.SEG_MOVETO: {
                            xo = x;
                            yo = y;
                            state = PathIterator.SEG_LINETO;
                            break;
                        }
                        case PathIterator.SEG_LINETO: {
                            if (path == null) {
                                int s = size - (i - 2*Polyline.DIMENSION);
                                for (int k=pi; ++k < polylines.length;) {
                                    final Polyline next = polylines[k];
                                    if (next != null) {
                                        s = Math.addExact(s, next.size);
                                    }
                                }
                                path = new Path2D.Double(Path2D.WIND_NON_ZERO, s / Polyline.DIMENSION);
                            }
                            path.moveTo(xo, yo);
                            path.lineTo(x, y);
                            state = PathIterator.SEG_CLOSE;
                            break;
                        }
                        default: {
                            if (Math.abs(x - xo) <= tolerance && Math.abs(y - yo) <= tolerance) {
                                path.closePath();
                                state = PathIterator.SEG_MOVETO;
                            } else {
                                path.lineTo(x, y);
                            }
                            break;
                        }
                    }
                }
                px = x;
                py = y;
            } while (i < size);
            p.clear();
        }
        return path;
    }
}
