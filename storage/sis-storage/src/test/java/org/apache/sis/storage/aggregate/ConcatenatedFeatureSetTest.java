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
package org.apache.sis.storage.aggregate;

import java.util.List;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.storage.MemoryFeatureSet;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertContentInfoEquals;
import static org.apache.sis.metadata.Assertions.assertFeatureSourceEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.metadata.iso.DefaultMetadata;


/**
 * Tests {@link ConcatenatedFeatureSet}.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final class ConcatenatedFeatureSetTest extends TestCase {
    /**
     * Tests the concatenation of two feature sets having the same feature type.
     *
     * @throws DataStoreException if an error occurred while concatenating the feature sets.
     */
    @Test
    public void testSameType() throws DataStoreException {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("City");
        builder.addAttribute(String.class).setName("name");
        builder.addAttribute(Integer.class).setName("population");

        final DefaultFeatureType ft = builder.build();
        final FeatureSet cfs = ConcatenatedFeatureSet.create(
                new MemoryFeatureSet(null, ft, List.of(ft.newInstance(), ft.newInstance())),
                new MemoryFeatureSet(null, ft, List.of(ft.newInstance())));

        assertSame("getType()", ft, cfs.getType());
        assertEquals("features.count()", 3, cfs.features(false).count());

        final Metadata md = cfs.getMetadata();
        assertNotNull("getMetadata()", md);
        assertContentInfoEquals("City", 3, (FeatureCatalogueDescription) getSingleton(md.getContentInfo()));
        final Lineage lineage = getSingleton(((DefaultMetadata) md).getResourceLineages());
        assertFeatureSourceEquals("City", new String[] {"City"}, getSingleton(lineage.getSources()));
    }

    /**
     * Tests the concatenation of two feature sets having different feature types.
     *
     * @throws DataStoreException if an error occurred while concatenating the feature sets.
     */
    @Test
    @DependsOnMethod("testSameType")
    public void testCommonSuperType() throws DataStoreException {
        /*
         * First, we prepare two types sharing a common ancestor. We'll create two types using same properties,
         * so we can ensure that all data is exposed upon traversal, not only data defined in the super type.
         */
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.addAttribute(Integer.class).setName("value");
        builder.setName("parent");

        final DefaultFeatureType superType = builder.build();

        builder.clear();
        builder.setSuperTypes(superType);
        builder.addAttribute(String.class).setName("label");

        final DefaultFeatureType t1 = builder.setName("t1").build();
        final DefaultFeatureType t2 = builder.setName("t2").build();

        // Populate a feature set for first type.
        final AbstractFeature t1f1 = t1.newInstance();
        t1f1.setPropertyValue("value", 2);
        t1f1.setPropertyValue("label", "first-first");

        final AbstractFeature t1f2 = t1.newInstance();
        t1f2.setPropertyValue("value", 3);
        t1f2.setPropertyValue("label", "first-second");

        final FeatureSet t1fs = new MemoryFeatureSet(null, t1, List.of(t1f1, t1f2));

        // Populate a feature set for second type.
        final AbstractFeature t2f1 = t2.newInstance();
        t2f1.setPropertyValue("value", 3);
        t2f1.setPropertyValue("label", "second-first");

        final AbstractFeature t2f2 = t2.newInstance();
        t2f2.setPropertyValue("value", 4);
        t2f2.setPropertyValue("label", "second-second");

        final FeatureSet t2fs = new MemoryFeatureSet(null, t2, List.of(t2f1, t2f2));
        /*
         * First, we'll test that total sum of value property is coherent with initialized features.
         * After that, we will ensure that we can get back the right labels for each subtype.
         */
        final FeatureSet set = ConcatenatedFeatureSet.create(t1fs, t2fs);
        final int sum = set.features(true)
                .mapToInt(f -> (int) f.getPropertyValue("value"))
                .sum();
        assertEquals("Sum of feature `value` property", 12, sum);

        final Object[] t1labels = set.features(false)
                .filter(f -> t1.equals(f.getType()))
                .map(f -> f.getPropertyValue("label"))
                .toArray();
        assertArrayEquals("First type labels", new String[] {"first-first", "first-second"}, t1labels);

        final Object[] t2labels = set.features(false)
                .filter(f -> t2.equals(f.getType()))
                .map(f -> f.getPropertyValue("label"))
                .toArray();
        assertArrayEquals("First type labels", new String[] {"second-first", "second-second"}, t2labels);
    }

    /**
     * Tests the concatenation of two feature sets having no common parent.
     * Creation of {@link ConcatenatedFeatureSet} is expected to fail.
     */
    @Test
    @DependsOnMethod("testCommonSuperType")
    public void noCommonType() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("super");
        final DefaultFeatureType mockSuperType = builder.build();
        final DefaultFeatureType firstType  = builder.setSuperTypes(mockSuperType).setName("first").build();
        final DefaultFeatureType secondType = builder.clear().setName("second").build();
        final FeatureSet fs1 = new MemoryFeatureSet(null, firstType,  List.of());
        final FeatureSet fs2 = new MemoryFeatureSet(null, secondType, List.of());
        try {
            FeatureSet concatenation = ConcatenatedFeatureSet.create(fs1, fs2);
            fail("Concatenation succeeded despite the lack of common type. Result is:\n" + concatenation);
        } catch (DataStoreContentException e) {
            // Expected behavior.
        } catch (DataStoreException e) {
            fail("Concatenation failed with an error reserved for other kind of error.");
        }
    }

    /**
     * Tests that no concatenated transform is created from less than 2 types.
     *
     * @throws DataStoreException if an error occurred while invoking the create method.
     */
    @Test
    public void lackOfInput() throws DataStoreException {
        try {
            final FeatureSet set = ConcatenatedFeatureSet.create();
            fail("An empty concatenation has been created:\n" + set);
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
        final FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("mock");
        final DefaultFeatureType mockType = builder.build();
        final FeatureSet fs1 = new MemoryFeatureSet(null, mockType, List.of());
        final FeatureSet set = ConcatenatedFeatureSet.create(fs1);
        assertSame("A concatenation has been created from a single type.", fs1, set);
    }
}
