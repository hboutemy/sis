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
package org.apache.sis.parameter;

import java.util.Collections;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.JAXBException;
import javax.measure.unit.SI;
import org.opengis.test.Validators;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import java.util.Objects;


/**
 * Tests XML (un)marshalling of {@link DefaultParameterValue}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(DefaultParameterValueTest.class)
public final strictfp class ParameterMarshallingTest extends XMLTestCase {
    /**
     * Creates a parameter value for marshalling test.
     */
    private static <T> DefaultParameterValue<T> create(final Class<T> type, final Range<?> valueDomain) {
        return new DefaultParameterValue<>(new DefaultParameterDescriptor<>(
                Collections.singletonMap(DefaultParameterDescriptor.NAME_KEY,
                        "A parameter of type " + type.getSimpleName()),
                1, 1, type, valueDomain, null, null));
    }

    /**
     * Marshals the given object, then unmarshals it and compare with the original value.
     *
     * @param parameter The parameter to marshal.
     * @param expected  The expected XML (ignoring {@code xmlns}).
     */
    private static void testMarshallAndUnmarshall(final DefaultParameterValue<?> parameter, final String expected)
            throws JAXBException
    {
        final String xml = XML.marshal(parameter);
        assertXmlEquals(expected, xml, "xmlns:*");
        final DefaultParameterValue<?> r = (DefaultParameterValue<?>) XML.unmarshal(xml);
        if (!Objects.deepEquals(parameter.getValue(), r.getValue())) {
            // If we enter in this block, then the line below should always fail.
            // But we use this assertion for getting a better error message.
            assertEquals("value", parameter.getValue(), r.getValue());
        }
        assertEquals("unit", parameter.getUnit(), r.getUnit());
        /*
         * Verify the descriptor, especially the 'valueClass' property. That property is not part of GML,
         * so Apache SIS has to rely on some tricks for finding this information (see CC_OperationParameter).
         */
        final ParameterDescriptor<?> reference = parameter.getDescriptor();
        final ParameterDescriptor<?> descriptor = r.getDescriptor();
        assertNotNull("descriptor",                                             descriptor);
        assertEquals ("descriptor.name",          reference.getName(),          descriptor.getName());
        assertEquals ("descriptor.unit",          reference.getUnit(),          descriptor.getUnit());
        assertEquals ("descriptor.valueClass",    reference.getValueClass(),    descriptor.getValueClass());
        assertEquals ("descriptor.minimumOccurs", reference.getMinimumOccurs(), descriptor.getMinimumOccurs());
        assertEquals ("descriptor.maximumOccurs", reference.getMaximumOccurs(), descriptor.getMaximumOccurs());
        Validators.validate(r);
    }

    /**
     * Tests (un)marshalling of a parameter descriptor.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testDescriptor() throws JAXBException {
        final DefaultParameterDescriptor<Double> descriptor = new DefaultParameterDescriptor<>(
                Collections.singletonMap(DefaultParameterDescriptor.NAME_KEY, "A descriptor"),
                0, 1, Double.class, null, null, null);
        final String xml = XML.marshal(descriptor);
        assertXmlEquals(
                "<gml:OperationParameter xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:name>A descriptor</gml:name>\n"
              + "  <gml:minimumOccurs>0</gml:minimumOccurs>\n"
              + "</gml:OperationParameter>", xml, "xmlns:*");
        final DefaultParameterDescriptor<?> r = (DefaultParameterDescriptor<?>) XML.unmarshal(xml);
        assertEquals("name", "A descriptor", r.getName().getCode());
        assertEquals("minimumOccurs", 0, r.getMinimumOccurs());
        assertEquals("maximumOccurs", 1, r.getMaximumOccurs());
        /*
         * A DefaultParameterDescriptor with null 'valueClass' is illegal, but there is no way we can guess
         * this information if the <gml:OperationParameter> element was not a child of <gml:ParameterValue>.
         * The current implementation leaves 'valueClass' to null despite being illegal. This behavior may
         * change in any future Apache SIS version.
         */
        assertNull("valueDomain", r.getValueDomain());
        assertNull("valueClass",  r.getValueClass());   // May change in any future SIS release.
    }

    /**
     * Tests (un)marshalling of a parameter with a string value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testDescriptor")
    public void testStringValue() throws JAXBException {
        final DefaultParameterValue<String> parameter = create(String.class, null);
        parameter.setValue("A string value");
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:stringValue>A string value</gml:stringValue>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type String</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }

    /**
     * Tests (un)marshalling of a parameter with a URI value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     * @throws URISyntaxException should never happen.
     */
    @Test
    @DependsOnMethod("testStringValue")
    public void testURIValue() throws JAXBException, URISyntaxException {
        final DefaultParameterValue<URI> parameter = create(URI.class, null);
        parameter.setValue(new URI("http://www.opengis.org"));
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:valueFile>http://www.opengis.org</gml:valueFile>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type URI</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }

    /**
     * Tests (un)marshalling of a parameter with an integer value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testStringValue")
    public void testBooleanValue() throws JAXBException {
        final DefaultParameterValue<Boolean> parameter = create(Boolean.class, null);
        parameter.setValue(Boolean.TRUE);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n" 
              + "  <gml:booleanValue>true</gml:booleanValue>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type Boolean</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }

    /**
     * Tests (un)marshalling of a parameter with an integer value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testStringValue")
    public void testIntegerValue() throws JAXBException {
        final DefaultParameterValue<Integer> parameter = create(Integer.class, null);
        parameter.setValue(2000);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n" 
              + "  <gml:integerValue>2000</gml:integerValue>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type Integer</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }

    /**
     * Tests (un)marshalling of a parameter with a list of integer values.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testStringValue")
    public void testIntegerValueList() throws JAXBException {
        final DefaultParameterValue<int[]> parameter = create(int[].class, null);
        parameter.setValue(new int[] {101, 105, 208});
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:integerValueList>101 105 208</gml:integerValueList>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type int[]</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }

    /**
     * Tests (un)marshalling of a parameter with a floating point value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testStringValue")
    public void testValue() throws JAXBException {
        final DefaultParameterValue<Double> parameter = create(Double.class,
                new MeasurementRange<Double>(Double.class, null, false, null, false, SI.METRE));
        parameter.setValue(3000, SI.METRE);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n" 
              + "  <gml:value uom=\"urn:ogc:def:uom:EPSG::9001\">3000.0</gml:value>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type Double</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }

    /**
     * Tests (un)marshalling of a parameter with a list of floating point values.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testStringValue")
    public void testValueList() throws JAXBException {
        final DefaultParameterValue<double[]> parameter = create(double[].class,
                new MeasurementRange<Double>(Double.class, null, false, null, false, SI.METRE));
        parameter.setValue(new double[] {203, 207, 204}, SI.METRE);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:valueList uom=\"urn:ogc:def:uom:EPSG::9001\">203.0 207.0 204.0</gml:valueList>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type double[]</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
    }
}
