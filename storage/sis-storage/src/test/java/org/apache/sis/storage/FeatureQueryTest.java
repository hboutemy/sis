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
package org.apache.sis.storage;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.storage.MemoryFeatureSet;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.filter.Expression;


/**
 * Tests {@link FeatureQuery} and (indirectly) {@link FeatureSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public final class FeatureQueryTest extends TestCase {
    /**
     * An arbitrary number of features, all of the same type.
     */
    private final AbstractFeature[] features;

    /**
     * The {@link #features} array wrapped in a in-memory feature set.
     */
    private final FeatureSet featureSet;

    /**
     * The query to be executed.
     */
    private final FeatureQuery query;

    /**
     * Creates a new test with a feature type composed of two attributes and one association.
     */
    public FeatureQueryTest() {
        FeatureTypeBuilder ftb;

        // A dependency of the test feature type.
        ftb = new FeatureTypeBuilder().setName("Dependency");
        ftb.addAttribute(Integer.class).setName("value3");
        final DefaultFeatureType dependency = ftb.build();

        // Test feature type with attributes and association.
        ftb = new FeatureTypeBuilder().setName("Test");
        ftb.addAttribute(Integer.class).setName("value1");
        ftb.addAttribute(Integer.class).setName("value2");
        ftb.addAssociation(dependency).setName("dependency");
        final DefaultFeatureType type = ftb.build();
        features = new AbstractFeature[] {
            feature(type, null,       3, 1,  0),
            feature(type, null,       2, 2,  0),
            feature(type, dependency, 2, 1, 25),
            feature(type, dependency, 1, 1, 18),
            feature(type, null,       4, 1,  0)
        };
        featureSet = new MemoryFeatureSet(null, type, Arrays.asList(features));
        query      = new FeatureQuery();
    }

    /**
     * Creates an instance of the test feature type with the given values.
     * The {@code value3} is stored only if {@code dependency} is non-null.
     */
    private static AbstractFeature feature(final DefaultFeatureType type, final DefaultFeatureType dependency,
                                   final int value1, final int value2, final int value3)
    {
        final AbstractFeature f = type.newInstance();
        f.setPropertyValue("value1", value1);
        f.setPropertyValue("value2", value2);
        if (dependency != null) {
            final AbstractFeature d = dependency.newInstance();
            d.setPropertyValue("value3", value3);
            f.setPropertyValue("dependency", d);
        }
        return f;
    }

    /**
     * Configures the query for returning a single instance and returns that instance.
     */
    private AbstractFeature executeAndGetFirst() throws DataStoreException {
        query.setLimit(1);
        final FeatureSet subset = query.execute(featureSet);
        return TestUtilities.getSingleton(subset.features(false).collect(Collectors.toList()));
    }

    /**
     * Executes the query and verify that the result is equal to the features at the given indices.
     *
     * @param  indices  indices of expected features.
     * @throws DataStoreException if an error occurred while executing the query.
     */
    private void verifyQueryResult(final int... indices) throws DataStoreException {
        final FeatureSet fs = query.execute(featureSet);
        final List<AbstractFeature> result = fs.features(false).collect(Collectors.toList());
        assertEquals("size", indices.length, result.size());
        for (int i=0; i<indices.length; i++) {
            final AbstractFeature expected = features[indices[i]];
            final AbstractFeature actual   = result.get(i);
            if (!expected.equals(actual)) {
                fail(String.format("Unexpected feature at index %d%n"
                                 + "Expected:%n%s%n"
                                 + "Actual:%n%s%n", i, expected, actual));
            }
        }
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setLimit(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testLimit() throws DataStoreException {
        query.setLimit(2);
        verifyQueryResult(0, 1);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setOffset(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testOffset() throws DataStoreException {
        query.setOffset(2);
        verifyQueryResult(2, 3, 4);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setSelection(Filter)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testSelection() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setSelection(ff.equal(ff.property("value1", Integer.class),
                                    ff.literal(2)));
        verifyQueryResult(1, 2);
    }

    /**
     * Tests {@link FeatureQuery#setSelection(Filter)} on complex features
     * with a filter that follows associations.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testSelectionThroughAssociation() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setSelection(ff.equal(ff.property("dependency/value3"), ff.literal(18)));
        verifyQueryResult(3);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setProjection(FeatureQuery.Column[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjection() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), (String) null),
                            new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), "renamed1"),
                            new FeatureQuery.NamedExpression(ff.literal("a literal"), "computed"));

        // Check result type.
        final AbstractFeature instance = executeAndGetFirst();
        final DefaultFeatureType resultType = instance.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(3, resultType.getProperties(true).size());
        final AbstractIdentifiedType pt1 = resultType.getProperty("value1");
        final AbstractIdentifiedType pt2 = resultType.getProperty("renamed1");
        final AbstractIdentifiedType pt3 = resultType.getProperty("computed");
        assertTrue(pt1 instanceof DefaultAttributeType);
        assertTrue(pt2 instanceof DefaultAttributeType);
        assertTrue(pt3 instanceof DefaultAttributeType);
        assertEquals(Integer.class, ((DefaultAttributeType) pt1).getValueClass());
        assertEquals(Integer.class, ((DefaultAttributeType) pt2).getValueClass());
        assertEquals(String.class,  ((DefaultAttributeType) pt3).getValueClass());

        // Check feature instance.
        assertEquals(3, instance.getPropertyValue("value1"));
        assertEquals(3, instance.getPropertyValue("renamed1"));
        assertEquals("a literal", instance.getPropertyValue("computed"));
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setProjection(String[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionByNames() throws DataStoreException {
        query.setProjection("value2");
        final AbstractFeature instance = executeAndGetFirst();
        final AbstractIdentifiedType p = TestUtilities.getSingleton(instance.getType().getProperties(true));
        assertEquals("value2", p.getName().toString());
    }

    /**
     * Tests the creation of default column names when no alias where explicitly specified.
     * Note that the string representations of default names shall be unlocalized.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testDefaultColumnName() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setLimit(1);
        query.setProjection(
                ff.add(ff.property("value1", Number.class), ff.literal(1)),
                ff.add(ff.property("value2", Number.class), ff.literal(1)));
        final FeatureSet subset = featureSet.subset(query);
        final DefaultFeatureType type = subset.getType();
        final Iterator<? extends AbstractIdentifiedType> properties = type.getProperties(true).iterator();
        assertEquals("Unnamed #1", properties.next().getName().toString());
        assertEquals("Unnamed #2", properties.next().getName().toString());
        assertFalse(properties.hasNext());

        final AbstractFeature instance = TestUtilities.getSingleton(subset.features(false).collect(Collectors.toList()));
        assertSame(type, instance.getType());
    }

    /**
     * Tests {@link FeatureQuery#setProjection(FeatureQuery.NamedExpression...)} on an abstract feature type.
     * We expect the column to be defined even if the property name is undefined on the feature type.
     * This case happens when the {@link FeatureSet} contains features with inherited types.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionOfAbstractType() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1"),  (String) null),
                            new FeatureQuery.NamedExpression(ff.property("/*/unknown"), "unexpected"));

        // Check result type.
        final AbstractFeature instance = executeAndGetFirst();
        final DefaultFeatureType resultType = instance.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(2, resultType.getProperties(true).size());
        final AbstractIdentifiedType pt1 = resultType.getProperty("value1");
        final AbstractIdentifiedType pt2 = resultType.getProperty("unexpected");
        assertTrue(pt1 instanceof DefaultAttributeType<?>);
        assertTrue(pt2 instanceof DefaultAttributeType<?>);
        assertEquals(Integer.class, ((DefaultAttributeType<?>) pt1).getValueClass());
        assertEquals(Object.class,  ((DefaultAttributeType<?>) pt2).getValueClass());

        // Check feature property values.
        assertEquals(3,    instance.getPropertyValue("value1"));
        assertEquals(null, instance.getPropertyValue("unexpected"));
    }

    /**
     * Tests {@link FeatureQuery#setProjection(FeatureQuery.NamedExpression...)} on complex features
     * with a filter that follows associations.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionThroughAssociation() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1"),  (String) null),
                            new FeatureQuery.NamedExpression(ff.property("dependency/value3"), "value3"));
        query.setOffset(2);
        final AbstractFeature instance = executeAndGetFirst();
        assertEquals("value1",  2, instance.getPropertyValue("value1"));
        assertEquals("value3", 25, instance.getPropertyValue("value3"));
    }

    /**
     * Shortcut for creating expression for a projection computed on-the-fly.
     */
    private static FeatureQuery.NamedExpression virtualProjection(final Expression<AbstractFeature, ?> expression, final String alias) {
        return new FeatureQuery.NamedExpression(expression, Names.createLocalName(null, null, alias), FeatureQuery.ProjectionType.COMPUTING);
    }

    /**
     * Verifies the effect of virtual projections.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testVirtualProjection() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(
                new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), (String) null),
                virtualProjection(ff.property("value1", Integer.class), "renamed1"),
                virtualProjection(ff.literal("a literal"), "computed"));

        // Check result type.
        final AbstractFeature instance = executeAndGetFirst();
        final DefaultFeatureType resultType = instance.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(3, resultType.getProperties(true).size());
        final AbstractIdentifiedType pt1 = resultType.getProperty("value1");
        final AbstractIdentifiedType pt2 = resultType.getProperty("renamed1");
        final AbstractIdentifiedType pt3 = resultType.getProperty("computed");
        assertTrue(pt1 instanceof DefaultAttributeType<?>);
        assertTrue(pt2 instanceof AbstractOperation);
        assertTrue(pt3 instanceof AbstractOperation);
        final AbstractIdentifiedType result2 = ((AbstractOperation) pt2).getResult();
        final AbstractIdentifiedType result3 = ((AbstractOperation) pt3).getResult();
        assertEquals(Integer.class, ((DefaultAttributeType<?>) pt1).getValueClass());
        assertTrue(result2 instanceof DefaultAttributeType<?>);
        assertTrue(result3 instanceof DefaultAttributeType<?>);
        assertEquals(Integer.class, ((DefaultAttributeType<?>) result2).getValueClass());
        assertEquals(String.class,  ((DefaultAttributeType<?>) result3).getValueClass());

        // Check feature instance.
        assertEquals(3, instance.getPropertyValue("value1"));
        assertEquals(3, instance.getPropertyValue("renamed1"));
        assertEquals("a literal", instance.getPropertyValue("computed"));

        // The `ValueReference` operation should have been optimized as a link.
        assertEquals("value1", Features.getLinkTarget(pt2).get());
        assertTrue(Features.getLinkTarget(pt1).isEmpty());
        assertTrue(Features.getLinkTarget(pt3).isEmpty());
    }

    /**
     * Verifies that a virtual projection on a missing field causes an exception.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testIncorrectVirtualProjection() throws DataStoreException {
        final DefaultFilterFactory<AbstractFeature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), (String) null),
                            virtualProjection(ff.property("valueMissing", Integer.class), "renamed1"));

        DataStoreContentException ex = assertThrows(DataStoreContentException.class, this::executeAndGetFirst);
        assertNotNull(ex.getMessage());
    }
}
