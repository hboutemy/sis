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
package org.apache.sis.internal.coveragejson;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.measure.Unit;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.internal.coveragejson.binding.Axe;
import org.apache.sis.internal.coveragejson.binding.Axes;
import org.apache.sis.internal.coveragejson.binding.Coverage;
import org.apache.sis.internal.coveragejson.binding.CoverageJsonObject;
import org.apache.sis.internal.coveragejson.binding.Domain;
import org.apache.sis.internal.coveragejson.binding.GeographicCRS;
import org.apache.sis.internal.coveragejson.binding.IdentifierRS;
import org.apache.sis.internal.coveragejson.binding.NdArray;
import org.apache.sis.internal.coveragejson.binding.Parameter;
import org.apache.sis.internal.coveragejson.binding.ProjectedCRS;
import org.apache.sis.internal.coveragejson.binding.ReferenceSystemConnection;
import org.apache.sis.internal.coveragejson.binding.TemporalRS;
import org.apache.sis.internal.coveragejson.binding.VerticalCRS;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.NoSuchDataException;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class CoverageResource extends AbstractGridCoverageResource {

    private static final DateTimeFormatter YEAR = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 1, 19, SignStyle.EXCEEDS_PAD)
                .toFormatter();


    private static final DateTimeFormatter YEAR_MONTH = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 1, 19, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 1)
                .toFormatter();

    private static final DateTimeFormatter YEAR_MONTH_DAY = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 1, 19, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 1)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter();

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    private final CoverageJsonStore store;
    private final Coverage binding;

    private final GridGeometry gridGeometry;
    private final List<SampleDimension> sampleDimensions;
    private final Map<String,double[]> datas;

    public CoverageResource(CoverageJsonStore store, Coverage binding) throws DataStoreException {
        super(null);
        this.store = store;
        this.binding = binding;

        //rebuild grid geometry
        try {
            gridGeometry = jsonToGridGeometry(binding.domain);
        } catch (FactoryException ex) {
            throw new DataStoreException("Failed to create GridGeometry from JSON Domain", ex);
        }
        //rebuild sample dimensions
        sampleDimensions = new ArrayList<>();
        for (Entry<String,Parameter> entry : binding.parameters.any.entrySet()) {
            final SampleDimension sd = jsonToSampleDimension(entry.getKey(), entry.getValue());
            sampleDimensions.add(sd);
        }
        if (binding.parameterGroups != null) {
            throw new UnsupportedOperationException("Parameter groups not supported yet.");
        }
        //read datas
        datas = new HashMap<>();
        for (Entry<String,NdArray> entry : binding.ranges.any.entrySet()) {
            datas.put(entry.getKey(), jsonToDataBuffer(entry.getValue()));
        }
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return gridGeometry;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return Collections.unmodifiableList(sampleDimensions);
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {

        final GridGeometry intersection;
        if (domain != null) {
            try {
                intersection = gridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(domain).build();
            } catch (DisjointExtentException ex) {
                throw new NoSuchDataException(ex);
            }
        } else {
            intersection = gridGeometry;
        }

        final double[][] rawDatas;
        final List<SampleDimension> selected;
        if (ranges == null || ranges.length == 0) {
            selected = sampleDimensions;
            rawDatas = new double[sampleDimensions.size()][0];
            for (int i = 0; i < rawDatas.length; i++) {
                rawDatas[i] = datas.get(sampleDimensions.get(i).getName().toString());
            }

        } else {
            selected = new ArrayList<>();
            rawDatas = new double[ranges.length][0];
            for (int i = 0; i < rawDatas.length; i++) {
                final SampleDimension sd = sampleDimensions.get(ranges[i]);
                selected.add(sd);
                rawDatas[i] = datas.get(sd.getName().toString());
            }
        }

        final DataBuffer buffer = new DataBufferDouble(rawDatas, rawDatas[0].length);
        return new BufferedGridCoverage(intersection, selected, buffer);
    }

    /**
     * Transform JSON domain to GridGeometry.
     */
    private static GridGeometry jsonToGridGeometry(Domain domain) throws DataStoreException, FactoryException {

        if (Domain.DOMAINTYPE_GRID.equalsIgnoreCase(domain.domainType)) {

            //build coordinate system
            final List<ReferenceSystemConnection> referencing = domain.referencing;
            final List<String> axeNames = new ArrayList<>();
            final List<CoordinateReferenceSystem> crss = new ArrayList<>();
            if (referencing != null && !referencing.isEmpty()) {
                for (ReferenceSystemConnection rsc : referencing) {
                    axeNames.addAll(rsc.coordinates);
                    final CoordinateReferenceSystem crs = jsonToCoordinateReferenceSystem(rsc.system);
                    if (crs.getCoordinateSystem().getDimension() != rsc.coordinates.size()) {
                        throw new DataStoreException("Declared CRS " + rsc.system.toString() + " do not match coordinates length");
                    }
                    crss.add(crs);
                }
            } else {
                throw new DataStoreException("Coverage domain must be defined, Coverage as part of CoverageCollection not supported yet.");
            }

            //build extent
            final int dimension = axeNames.size();
            final Axes axes = domain.axes;
            final GridGeometry[] axeGrids = new GridGeometry[dimension];

            //check if axes declared on crs are ordered in the same way as the grid extent.
            final int[] reorder = new int[dimension];
            boolean inOrder = true;

            for (int i = 0; i < dimension; i++) {
                final String axeName = axeNames.get(i);
                final Axe axe;
                final int realIdx;
                switch (axeName) {
                    case "x" :
                        if (axes.x == null) throw new DataStoreException("X axe is undefined");
                        axe = axes.x;
                        realIdx = 0;
                        reorder[i] = realIdx;
                        inOrder &= (i == realIdx);
                        break;
                    case "y" :
                        if (axes.y == null) throw new DataStoreException("Y axe is undefined");
                        axe = axes.y;
                        realIdx = 1;
                        reorder[i] = realIdx;
                        inOrder &= (i == realIdx);
                        break;
                    case "z" :
                        if (axes.z == null) throw new DataStoreException("Z axe is undefined");
                        axe = axes.z;
                        realIdx = 2;
                        reorder[i] = realIdx;
                        inOrder &= (i == realIdx);
                        break;
                    case "t" :
                        if (axes.t == null) throw new DataStoreException("T axe is undefined");
                        axe = axes.t;
                        realIdx = reorder.length == 3 ? 2 : 3;
                        reorder[i] = realIdx;
                        inOrder &= (i == realIdx);
                        break;
                    default: throw new DataStoreException("Unexpected axe name :" + axeName);
                }
                axeGrids[realIdx] = jsonAxeToGridGeometry(axeName, axe);
            }

            final DimensionNameType[] dnt = new DimensionNameType[dimension];
            final long[] lower = new long[dimension];
            final long[] upper = new long[dimension]; //inclusive
            MathTransform gridToCrs = null;
            for (int i = 0; i < dimension; i++) {
                dnt[i] = axeGrids[i].getExtent().getAxisType(0).get();
                upper[i] = axeGrids[i].getExtent().getHigh(0);
                if (gridToCrs == null) {
                    gridToCrs = axeGrids[i].getGridToCRS(PixelInCell.CELL_CENTER);
                } else {
                    gridToCrs = MathTransforms.compound(gridToCrs, axeGrids[i].getGridToCRS(PixelInCell.CELL_CENTER));
                }
            }


            if (!inOrder) {
                final MatrixSIS m = Matrices.createZero(dimension+1, dimension+1);
                for (int i = 0; i < dimension; i++) {
                    m.setElement(i, reorder[i], 1.0);
                }
                m.setElement(dimension, dimension, 1.0);
                final MathTransform reorderTrs = MathTransforms.linear(m);
                gridToCrs = MathTransforms.concatenate(reorderTrs, gridToCrs);
            }


            final CoordinateReferenceSystem crs = CRS.compound(crss.toArray(CoordinateReferenceSystem[]::new));
            final GridExtent extent = new GridExtent(dnt, lower, upper, true);
            return new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);

        } else {
            throw new DataStoreException("Unsupported domain type " + domain.domainType);
        }
    }

    /**
     * Transform JSON axe to 1D GridGeometry without CRS.
     */
    private static GridGeometry jsonAxeToGridGeometry(String axeName, Axe axe) throws DataStoreException {

        if (axe.dataType == null || Axe.DATATYPE_PRIMITIVE.equals(axe.dataType)) {

        } else if (Axe.DATATYPE_TUPLE.equals(axe.dataType) ) {
            throw new UnsupportedOperationException("Tuple axe data type not supported yet.");
        } else if (Axe.DATATYPE_POLYGON.equals(axe.dataType) ) {
            throw new UnsupportedOperationException("Polygon axe data type not supported yet.");
        } else {
            throw new DataStoreException("Unexpected axe data type :" + axe.dataType);
        }

        //rebuild axe transform
        final MathTransform1D axeTrs;
        final int size;
        if (axe.values != null) {
            final double[] values = new double[axe.values.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = asDouble(axe.values.get(i));
            }
            size = values.length;
            axeTrs = MathTransforms.interpolate(null, values);
        } else if (axe.start != null) {
            size = axe.num;
            if (axe.num == 1) {
                axeTrs = (MathTransform1D) MathTransforms.linear(1.0, axe.start);
            } else {
                final double step = (axe.stop - axe.start) / (axe.num -1);
                axeTrs = (MathTransform1D) MathTransforms.linear(step, axe.start);
            }
        } else {
            throw new DataStoreException("Axe must have values or star/stop values");
        }

        final GridExtent extent = new GridExtent(new DimensionNameType[]{DimensionNameType.valueOf(axeName)}, new long[]{0}, new long[]{size-1}, true);
        return new GridGeometry(extent, PixelInCell.CELL_CENTER, axeTrs, null);
    }

    /**
     * Transform JSON system object to CoordinateReferenceSystem.
     */
    private static CoordinateReferenceSystem jsonToCoordinateReferenceSystem(CoverageJsonObject obj) throws FactoryException {
        if (obj instanceof GeographicCRS) {
            final GeographicCRS jcrs = (GeographicCRS) obj;
            if (jcrs.id != null) {
                if (jcrs.id.equals("http://www.opengis.net/def/crs/EPSG/0/4979")) {
                    return CommonCRS.WGS84.geographic3D();
                }
                return CRS.forCode(jcrs.id);
            } else {
                throw new UnsupportedOperationException("Geographic CRS wihout id not supported");
            }
        } else if (obj instanceof ProjectedCRS) {
            final ProjectedCRS jcrs = (ProjectedCRS) obj;
            throw new UnsupportedOperationException("ProjectedCRS not supported yet");

        } else if (obj instanceof VerticalCRS) {
            final VerticalCRS jcrs = (VerticalCRS) obj;
            throw new UnsupportedOperationException("VerticalCRS not supported yet");

        } else if (obj instanceof TemporalRS) {
            final TemporalRS jcrs = (TemporalRS) obj;
            if (jcrs.timeScale != null) {
                throw new UnsupportedOperationException("TemporalRS timeScale not supported yet");
            }
            if ("Gregorian".equalsIgnoreCase(jcrs.calendar)) {
                return CommonCRS.Temporal.JAVA.crs();
            } else {
                throw new UnsupportedOperationException(jcrs.calendar + "calendar not supported yet");
            }

        } else if (obj instanceof IdentifierRS) {
            final IdentifierRS jcrs = (IdentifierRS) obj;
            throw new UnsupportedOperationException("IdentifierRS not supported yet");

        } else {
            throw new UnsupportedOperationException("Unsupported system " + String.valueOf(obj));
        }
    }

    /**
     * Transform JSON parameter to SampleDimension.
     */
    private static SampleDimension jsonToSampleDimension(String name, Parameter parameter) {
        final SampleDimension.Builder builder = new SampleDimension.Builder();

        builder.setName(name);

//        if (parameter.id != null) {
//            builder.setName(parameter.id);
//        } else if (parameter.label != null) {
//            builder.setName(parameter.label);
//        } else if (parameter.description != null) {
//            builder.setName(parameter.description);
//        }

        Unit unit = jsonToUnit(parameter.unit);

        //TODO categories
        //parameter.categoryEncoding;
        //parameter.observedProperty;

        return builder.build();
    }

    /**
     * Transform JSON unit to SIS Unit.
     */
    private static Unit jsonToUnit(org.apache.sis.internal.coveragejson.binding.Unit unit) {
        if (unit == null) return Units.UNITY;

        if (unit.symbol instanceof String) {
            return Units.valueOf(unit.symbol.toString());
        }
        return Units.UNITY;
    }

    /**
     * Transform JSON NdArray to number array.
     */
    private static double[] jsonToDataBuffer(NdArray array) throws DataStoreException {
        //TODO more work on checking axes order
        double[] values = new double[array.values.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = asDouble(array.values.get(i));
        }

        return values;
    }

    private static double asDouble(Object cdt) throws DataStoreException {
        if (cdt == null) {
            return Double.NaN;
        } else if (cdt instanceof String) {
            final Instant instant = parseDataTime(String.valueOf(cdt));
            return instant.toEpochMilli();
        } else if (cdt instanceof Number) {
            return ((Number) cdt).doubleValue();
        } else {
            throw new DataStoreException("Unexpected value : " + cdt);
        }
    }

    /**
     * If the calendar is based on years, months, days, then the referenced
     * values SHOULD use one of the following ISO8601-based lexical representations:
     * YYYY
     * ±XYYYY (where X stands for extra year digits)
     * YYYY-MM
     * YYYY-MM-DD
     * YYYY-MM-DDTHH:MM:SS[.F]Z where Z is either “Z” or a time scale offset +|-HH:MM
     *
     * If calendar dates with reduced precision are used in a lexical
     * representation (e.g. "2016"), then a client SHOULD interpret those dates
     * in that reduced precision.
     */
    private static Instant parseDataTime(String str) throws DataStoreException {

        for (DateTimeFormatter dtf : Arrays.asList(YEAR,YEAR_MONTH, YEAR_MONTH_DAY, DATE_TIME)) {
            try {
                TemporalAccessor accesser = dtf.parse(str);
                return Instant.from(accesser);
            } catch (DateTimeParseException ex) {
                //do nothing
            }
        }
        throw new DataStoreException("Unable to parse date : " + str);
    }
}
