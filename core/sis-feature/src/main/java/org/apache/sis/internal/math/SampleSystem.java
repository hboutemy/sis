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
package org.apache.sis.internal.math;

import java.util.Arrays;
import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.iso.Names;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

/**
 * Experimental class to store multisamples dimensions.
 *
 * This serves an identical purpose as SampleDimension but usable with geometry attributes.
 *
 * Waiting for a proper implementation in SIS when reviewing ISO 19123 / 2153.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SampleSystem {

    private static final GenericName UNNAMED = Names.createLocalName(null, null, "unnamed");
    private static final SampleSystem UNDEFINED_1S = new SampleSystem(1);
    private static final SampleSystem UNDEFINED_2S = new SampleSystem(2);
    private static final SampleSystem UNDEFINED_3S = new SampleSystem(3);
    private static final SampleSystem UNDEFINED_4S = new SampleSystem(4);
    private static SampleSystem[] UNDEFINED = new SampleSystem[0];
    private static Cache<CoordinateReferenceSystem, SampleSystem> CACHE = new Cache<>();

    private final GenericName name;
    private final CoordinateReferenceSystem crs;
    private final int size;

    private SampleSystem(int size) {
        ArgumentChecks.ensureStrictlyPositive("size", size);
        this.name = UNNAMED;
        this.crs = null;
        this.size = size;
    }

    private SampleSystem(CoordinateReferenceSystem crs) {
        this(UNNAMED, crs);
    }

    private SampleSystem(GenericName name, CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("crs", crs);
        this.name = name;
        this.crs = crs;
        this.size = crs.getCoordinateSystem().getDimension();
    }

    public static SampleSystem ofSize(int nbDim) {
        ArgumentChecks.ensureStrictlyPositive("nbDim", nbDim);
        switch (nbDim) {
            case 1 : return UNDEFINED_1S;
            case 2 : return UNDEFINED_2S;
            case 3 : return UNDEFINED_3S;
            case 4 : return UNDEFINED_4S;
            default: {
                final int idx = nbDim - 4;
                synchronized (UNNAMED) {
                    if (idx >= UNDEFINED.length) {
                        UNDEFINED = Arrays.copyOf(UNDEFINED, idx+1);
                    }
                    if (UNDEFINED[idx] == null) {
                        UNDEFINED[idx] = new SampleSystem(nbDim);
                    }
                    return UNDEFINED[idx];
                }
            }
        }
    }

    public static SampleSystem of(CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("crs", crs);
        try {
            return CACHE.getOrCreate(crs, () -> new SampleSystem(crs));
        } catch (Exception ex) {
            throw new BackingStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns an identification for this dimension. This is typically used as a way to perform a band select
     * by using human comprehensible descriptions instead of just numbers.
     *
     * @return an identification of this system, nerver null.
     */
    public GenericName getName() {
        return name;
    }

    /**
     * Returns the coordinate reference system for this record dimension if it is a coordinate system.
     *
     * @return can be null.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Returns the size in number of samples in this dimension.
     *
     * @return dimension size
     */
    public int getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, crs, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SampleSystem other = (SampleSystem) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.crs, other.crs);
    }

}
