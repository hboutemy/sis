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
package org.apache.sis.referencing.crs;

import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.test.Validators;
import org.apache.sis.referencing.GeodeticObjectVerifier;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link DefaultGeodeticCRS} class.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.4
 */
@DependsOn({
    AbstractCRSTest.class,
    org.apache.sis.referencing.cs.DefaultEllipsoidalCSTest.class,
    org.apache.sis.referencing.datum.DefaultGeodeticDatumTest.class
})
public final class DefaultGeodeticCRSTest extends TestCase {
    /**
     * Opens the stream to the XML file in this package containing a geodetic CRS definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultGeodeticCRSTest.class.getResourceAsStream("GeographicCRS.xml");
    }

    /**
     * Tests (un)marshalling of a geodetic coordinate reference system.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultGeodeticCRS crs = unmarshalFile(DefaultGeodeticCRS.class, openTestFile());
        Validators.validate(crs);
        GeodeticObjectVerifier.assertIsWGS84(crs, false, true);
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        assertEquals("scope", "Horizontal component of 3D system.", crs.getScope().toString());
        /*
         * Marshal and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), crs, "xmlns:*", "xsi:schemaLocation");
    }
}
