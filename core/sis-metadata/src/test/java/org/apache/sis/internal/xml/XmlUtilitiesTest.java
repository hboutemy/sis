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
package org.apache.sis.internal.xml;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.Year;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;


/**
 * Test {@link XmlUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.3
 */
public final class XmlUtilitiesTest extends TestCase {
    /**
     * Tests the {@link XmlUtilities#toXML(Context, Date)} method.
     * This test arbitrarily uses the CET timezone.
     * The reverse operation is also tested.
     *
     * @throws DatatypeConfigurationException if the XML factory cannot be created.
     */
    @Test
    public void testDateToXML() throws DatatypeConfigurationException {
        createContext(false, Locale.FRANCE, "CET");
        final Date date = new Date(1230786000000L);
        final XMLGregorianCalendar calendar = XmlUtilities.toXML(context, date);
        assertEquals("2009-01-01T06:00:00.000+01:00", calendar.toString());
        assertEquals(date, XmlUtilities.toDate(context, calendar));

        calendar.setMillisecond(FIELD_UNDEFINED);
        assertEquals("2009-01-01T06:00:00+01:00", calendar.toString());
    }

    /**
     * Tests the {@link XmlUtilities#toXML(Context, Temporal)} method.
     * This test arbitrarily uses the JST timezone.
     *
     * @throws DatatypeConfigurationException if the XML factory cannot be created.
     */
    @Test
    public void testTemporalToXML() throws DatatypeConfigurationException {
        createContext(false, Locale.JAPAN, "JST");
        XMLGregorianCalendar calendar;
        Temporal t;

        t = Instant.ofEpochMilli(1230786000000L);
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("2009-01-01T14:00:00.000+09:00", calendar.toString());

        t = OffsetDateTime.parse("2009-01-01T06:00:00+01:00");
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("2009-01-01T06:00:00.000+01:00", calendar.toString());
        assertEquals(t, XmlUtilities.toTemporal(context, calendar));

        t = LocalDateTime.parse("2009-08-12T06:20:10");
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("2009-08-12T06:20:10.000", calendar.toString());
        assertEquals(t, XmlUtilities.toTemporal(context, calendar));

        t = LocalTime.parse("06:10:45");
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("06:10:45.000", calendar.toString());
        assertEquals(t, XmlUtilities.toTemporal(context, calendar));

        t = LocalDate.parse("2009-05-08");
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("2009-05-08", calendar.toString());
        assertEquals(t, XmlUtilities.toTemporal(context, calendar));

        t = YearMonth.parse("2009-05");
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("2009-05", calendar.toString());
        assertEquals(t, XmlUtilities.toTemporal(context, calendar));

        t = Year.parse("2012");
        calendar = XmlUtilities.toXML(context, t);
        assertEquals("2012", calendar.toString());
        assertEquals(t, XmlUtilities.toTemporal(context, calendar));
    }
}
