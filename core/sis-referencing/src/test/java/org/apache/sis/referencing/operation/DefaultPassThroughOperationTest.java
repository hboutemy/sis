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
package org.apache.sis.referencing.operation;

import java.util.List;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Transformation;
import org.opengis.test.Validators;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.GeoapiAssert.assertIdentifierEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link DefaultPassThroughOperation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.7
 */
@DependsOn({
    DefaultTransformationTest.class,
    SingleOperationMarshallingTest.class
})
public final class DefaultPassThroughOperationTest extends TestCase {
    /**
     * Opens the stream to the XML file in this package containing a projected CRS definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultPassThroughOperationTest.class.getResourceAsStream("PassThroughOperation.xml");
    }

    /**
     * Tests (un)marshalling of a concatenated operation.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultPassThroughOperation toTest = unmarshalFile(DefaultPassThroughOperation.class, openTestFile());
        Validators.validate(toTest);
        final CoordinateReferenceSystem sourceCRS = toTest.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = toTest.getTargetCRS();
        final CoordinateOperation       operation = toTest.getOperation();
        assertIdentifierEquals(          "identifier", "test", "test", null, "passthrough", getSingleton(toTest   .getIdentifiers()));
        assertIdentifierEquals("sourceCRS.identifier", "test", "test", null, "source",      getSingleton(sourceCRS.getIdentifiers()));
        assertIdentifierEquals("targetCRS.identifier", "test", "test", null, "target",      getSingleton(targetCRS.getIdentifiers()));
        assertIdentifierEquals("operation.identifier", "test", "test", null, "rotation",    getSingleton(operation.getIdentifiers()));
        assertInstanceOf("sourceCRS",    CompoundCRS.class, sourceCRS);
        assertInstanceOf("targetCRS",    CompoundCRS.class, targetCRS);
        assertInstanceOf("operation", Transformation.class, operation);
        final List<CoordinateReferenceSystem> srcComponents = ((CompoundCRS) sourceCRS).getComponents();
        final List<CoordinateReferenceSystem> tgtComponents = ((CompoundCRS) targetCRS).getComponents();
        assertEquals("sourceCRS.components.size", 2, srcComponents.size());
        assertEquals("targetCRS.components.size", 2, tgtComponents.size());
        assertSame  ("sourceCRS.components[0]", operation.getSourceCRS(), srcComponents.get(0));
        assertSame  ("targetCRS.components[0]", operation.getTargetCRS(), tgtComponents.get(0));
        assertSame  ("targetCRS.components[1]", srcComponents.get(1),     tgtComponents.get(1));
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), toTest, "xmlns:*", "xsi:schemaLocation");
    }
}
