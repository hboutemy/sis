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
package org.apache.sis.metadata.iso.identification;

import java.util.Set;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.util.NameFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.geoapi.evolution.UnsupportedCodeList;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.xml.NilReason;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link DefaultServiceIdentification}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.5
 */
@DependsOn({
    DefaultCoupledResourceTest.class,
    org.apache.sis.metadata.iso.identification.DefaultDataIdentificationTest.class
})
public final class DefaultServiceIdentificationTest extends TestUsingFile {
    /**
     * Opens the stream to the XML file containing a service identification.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final Format format) {
        return format.openTestFile("ServiceIdentification.xml");
    }

    /**
     * Creates the service identification to use for testing purpose.
     */
    private static DefaultServiceIdentification create() {
        final NameFactory factory = DefaultNameFactory.provider();
        final DefaultCoupledResource resource = DefaultCoupledResourceTest.create(factory);
        resource.setResourceReferences(Set.of(new DefaultCitation("WMS specification")));
        final DefaultServiceIdentification id = new DefaultServiceIdentification(
                factory.createGenericName(null, "Web Map Server"),      // serviceType
                NilReason.MISSING.createNilObject(Citation.class),      // citation
                "A dummy service for testing purpose.");                // abstract
        id.setServiceTypeVersions(Set.of("1.0"));
        id.setCoupledResources(Set.of(resource));
        id.setCouplingType(UnsupportedCodeList.valueOf("LOOSE"));
        id.setContainsOperations(Set.of(resource.getOperation()));
        return id;
    }

    /**
     * Compare values of the given service identifications against the value expected for the
     * instance created by {@link #create()} method.
     */
    private static void verify(final DefaultServiceIdentification id) {
        assertEquals("serviceTypeVersion", "1.0",                                  getSingleton(id.getServiceTypeVersions()));
        assertEquals("serviceType",        "Web Map Server",                       String.valueOf(id.getServiceType()));
        assertEquals("abstract",           "A dummy service for testing purpose.", String.valueOf(id.getAbstract()));
        assertEquals("citation",           NilReason.MISSING,                      NilReason.forObject(id.getCitation()));
        assertEquals("couplingType",       UnsupportedCodeList.valueOf("loose"),   id.getCouplingType());

        final DefaultCoupledResource resource = getSingleton(id.getCoupledResources());
//      assertEquals("scopedName",        "mySpace:ABC-123",   …)  skipped because not present in new ISO 19115-3:2016.
//      assertEquals("resourceReference", "WMS specification", …)  skipped because not present in legacy ISO 19139:2007.

        final DefaultOperationMetadata op = resource.getOperation();
        assertNotNull("operation", op);
        assertEquals("operationName", "Get Map", op.getOperationName());
        assertEquals("distributedComputingPlatform", UnsupportedCodeList.valueOf("WEB_SERVICES"), getSingleton(op.getDistributedComputingPlatforms()));
        assertEquals("connectPoints", NilReason.MISSING, NilReason.forObject(getSingleton(op.getConnectPoints())));

        final ParameterDescriptor<?> param = getSingleton(op.getParameters());
        assertEquals("name", "My service parameter", String.valueOf(param.getName()));
        assertEquals("minimumOccurs", 0, param.getMinimumOccurs());
        assertEquals("maximumOccurs", 1, param.getMaximumOccurs());
    }

    /**
     * Tests the unmarshalling of a service metadata.
     *
     * @throws JAXBException if an error occurred during the during unmarshalling process.
     */
    @Test
    public void testUnmarshal() throws JAXBException {
        final DefaultServiceIdentification id = unmarshalFile(DefaultServiceIdentification.class, openTestFile(Format.XML2016));
        verify(id);
        final DefaultCoupledResource resource = getSingleton(id.getCoupledResources());
        assertTitleEquals("resourceReference", "WMS specification", getSingleton(resource.getResourceReferences()));
    }

    /**
     * Tests the unmarshalling of a service metadata from legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during the during unmarshalling process.
     */
    @Test
    public void testUnmarshalLegacy() throws JAXBException {
        final DefaultServiceIdentification id = unmarshalFile(DefaultServiceIdentification.class, openTestFile(Format.XML2007));
        verify(id);
        final DefaultCoupledResource resource = getSingleton(id.getCoupledResources());
        assertEquals("scopedName", "mySpace:ABC-123", String.valueOf(resource.getScopedName()));
    }

    /**
     * Tests the marshalling of a service metadata.
     *
     * @throws JAXBException if an error occurred during the during marshalling process.
     */
    @Test
    public void testMarshal() throws JAXBException {
        assertMarshalEqualsFile(openTestFile(Format.XML2016), create(), "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests the marshalling of a service metadata to legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during the during marshalling process.
     */
    @Test
    public void testMarshalLegacy() throws JAXBException {
        assertMarshalEqualsFile(openTestFile(Format.XML2007), create(), VERSION_2007, "xmlns:*", "xsi:schemaLocation");
    }
}
