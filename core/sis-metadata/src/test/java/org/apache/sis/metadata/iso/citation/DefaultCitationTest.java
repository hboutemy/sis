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
package org.apache.sis.metadata.iso.citation;

import java.net.URI;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.UnmodifiableMetadataException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.junit.Assert.*;


/**
 * Tests {@link DefaultCitation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
public final class DefaultCitationTest extends TestUsingFile {
    /**
     * Opens the stream to the XML file containing a citation.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final Format format) {
        return format.openTestFile("Citation.xml");
    }

    /**
     * Creates a citation with an arbitrary title, presentation form and other properties.
     *
     * @return an arbitrary citation.
     *
     * @since 0.7
     */
    public static DefaultCitation create() {
        final DefaultCitation citation = new DefaultCitation();
        final DefaultInternationalString title = new DefaultInternationalString();
        title.add(Locale.JAPANESE, "アンダーカレント");
        title.add(Locale.ENGLISH,  "Undercurrent");
        citation.setTitle(title);
        citation.setISBN("9782505004509");
        citation.setPresentationForms(List.of(
                PresentationForm.DOCUMENT_HARDCOPY,
                PresentationForm.DOCUMENT_DIGITAL));
        citation.setAlternateTitles(Set.of(
                new SimpleInternationalString("Andākarento")));   // Actually a different script of the Japanese title.

        final DefaultResponsibleParty author = new DefaultResponsibleParty(Role.AUTHOR);
        author.setParties(Set.of(new DefaultIndividual("Testsuya Toyoda", null, null)));

        final DefaultResponsibleParty editor = new DefaultResponsibleParty(Role.valueOf("EDITOR"));
        editor.setParties(Set.of(new DefaultOrganisation("Kōdansha", null, null, null)));
        editor.setExtents(Set.of(Extents.WORLD));

        citation.setCitedResponsibleParties(List.of(author, editor));
        return citation;
    }

    /**
     * Tests the identifier map, which handles ISBN and ISSN codes in a special way.
     */
    @Test
    public void testIdentifierMap() {
        final DefaultCitation citation = new DefaultCitation();
        final Collection<Identifier> identifiers = citation.getIdentifiers();
        final IdentifierMap identifierMap = citation.getIdentifierMap();
        assertTrue("Expected an initially empty set of identifiers.", identifiers.isEmpty());
        /*
         * Set the ISBN code, and ensure that the the ISBN is reflected in the identifier map.
         */
        citation.setISBN("MyISBN");
        assertEquals("MyISBN", citation.getISBN());
        assertEquals("ISBN code shall be included in the set of identifiers.", 1, identifiers.size());
        assertEquals("{ISBN=“MyISBN”}", identifierMap.toString());
        /*
         * Set the identifiers with a list containing ISBN and ISSN codes.
         * The ISBN code shall be ignored because and ISBN property was already set.
         * The ISSN code shall be retained because it is a new code.
         */
        assertNull("ISSN shall be initially null.", citation.getISSN());
        citation.setIdentifiers(List.of(
                new DefaultIdentifier(Citations.NETCDF, "MyNetCDF"),
                new DefaultIdentifier(Citations.EPSG,   "MyEPSG"),
                new DefaultIdentifier(Citations.ISBN,   "NewISBN"),
                new DefaultIdentifier(Citations.ISSN,   "MyISSN")));

        assertEquals("The ISBN value shall have been overwritten.",       "NewISBN", citation.getISBN());
        assertEquals("The ISSN value shall have been added, because new.", "MyISSN", citation.getISSN());
        assertEquals("{NetCDF=“MyNetCDF”, EPSG=“MyEPSG”, ISBN=“NewISBN”, ISSN=“MyISSN”}", identifierMap.toString());
    }

    /**
     * Tests {@link DefaultCitation#transitionTo(DefaultCitation.State)} to the final state.
     */
    @Test
    public void testTransitionToFinal() {
        final DefaultCitation original = create();
        final DefaultCitation clone = create();
        clone.transitionTo(DefaultCitation.State.FINAL);
        assertEquals("original.state", DefaultCitation.State.EDITABLE, original.state());
        assertEquals("clone.state",    DefaultCitation.State.FINAL,    clone.state());
        assertEquals(original, clone);
        SimpleInternationalString title = new SimpleInternationalString("Undercurrent");
        original.setTitle(title);
        try {
            clone.setTitle(title);
            fail("Frozen metadata shall not be modifiable.");
        } catch (UnmodifiableMetadataException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests {@link MetadataCopier} on a citation.
     */
    public void testCopy() {
        final DefaultCitation original = create();
        final DefaultCitation clone = (DefaultCitation) new MetadataCopier(MetadataStandard.ISO_19115).copy(original);
        assertCopy(original, clone);
    }

    /**
     * Verifies that {@code clone} is a copy of {@code original}, sharing same instance of values when possible.
     */
    private static void assertCopy(final DefaultCitation original, final DefaultCitation clone) {
        assertNotSame(original, clone);
        assertSame ("ISBN",  original.getISBN(),  clone.getISBN());
        assertSame ("title", original.getTitle(), clone.getTitle());
        assertSame ("alternateTitle", getSingleton(original.getAlternateTitles()),
                                     getSingleton(clone.getAlternateTitles()));

        assertCopy(original.getIdentifiers(),             clone.getIdentifiers());
        assertCopy(original.getCitedResponsibleParties(), clone.getCitedResponsibleParties());
        assertCopy(original.getPresentationForms(),       clone.getPresentationForms());
        /*
         * Verify the unique identifier, which is the ISBN code. ISBN and ISSN codes are handled
         * in a special way by DefaultCitation (they are instances of SpecializedIdentifier), but
         * the should nevertheless be cloned.
         */
        final Identifier ide = getSingleton(original.getIdentifiers());
        final Identifier ida = getSingleton(   clone.getIdentifiers());
        assertNotSame("identifier", ide, ida);
        assertSame("code",      ide.getCode(),      ida.getCode());
        assertSame("authority", ide.getAuthority(), ida.getAuthority());
        /*
         * Verify the author metadata.
         */
        final ResponsibleParty re = CollectionsExt.first(original.getCitedResponsibleParties());
        final ResponsibleParty ra = CollectionsExt.first(clone   .getCitedResponsibleParties());
        assertNotSame("citedResponsibleParty", re, ra);
        assertSame("role", re.getRole(), ra.getRole());
        assertSame("name", re.getIndividualName(),
                           ra.getIndividualName());
    }

    /**
     * Verifies that {@code actual} is an unmodifiable copy of {@code expected}.
     */
    private static <T> void assertCopy(final Collection<T> expected, final Collection<T> actual) {
        assertNotSame("ModifiableMetadata.transitionTo(FINAL) shall have copied the collection.", expected, actual);
        assertEquals("The copied collection shall have the same content than the original.", expected, actual);
        try {
            actual.add(null);
            fail("The copied collection shall be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests XML marshalling using the format derived form ISO 19115:2014 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @since 1.0
     */
    @Test
    public void testMarshalling() throws JAXBException {
        testMarshalling(Format.XML2016);
    }

    /**
     * Tests XML marshalling using the format derived form ISO 19115:2003 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testMarshalling")
    public void testMarshallingLegacy() throws JAXBException {
        testMarshalling(Format.XML2007);
    }

    /**
     * Tests XML marshalling for the given metadata version.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     */
    private void testMarshalling(final Format format) throws JAXBException {
        final DefaultOnlineResource rs = new DefaultOnlineResource(URI.create("https://tools.ietf.org/html/rfc1149"));
        rs.setName("IP over Avian Carriers");
        rs.setDescription(new SimpleInternationalString("High delay, low throughput, and low altitude service."));
        rs.setFunction(OnLineFunction.OFFLINE_ACCESS);

        final DefaultContact contact = new DefaultContact(rs);
        contact.setContactInstructions(new SimpleInternationalString("Send carrier pigeon."));
        contact.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "ip-protocol");
        final DefaultCitation c = new DefaultCitation("Fight against poverty");
        final DefaultResponsibleParty r1 = new DefaultResponsibleParty(Role.ORIGINATOR);
        final DefaultResponsibleParty r2 = new DefaultResponsibleParty(Role.valueOf("funder"));
        r1.setParties(Set.of(new DefaultIndividual("Maid Marian", null, contact)));
        r2.setParties(Set.of(new DefaultIndividual("Robin Hood",  null, contact)));
        c.setCitedResponsibleParties(List.of(r1, r2));
        c.getDates().add(new DefaultCitationDate(TestUtilities.date("2015-10-17 00:00:00"), DateType.valueOf("adopted")));
        c.getPresentationForms().add(PresentationForm.valueOf("physicalObject"));
        /*
         * Check that XML file built by the marshaller is the same as the example file.
         */
        assertMarshalEqualsFile(openTestFile(format), c, format.schemaVersion, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML unmarshalling using the format derived form ISO 19115:2014 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     *
     * @since 1.0
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        testUnmarshalling(Format.XML2016);
    }

    /**
     * Tests XML unmarshalling using the format derived form ISO 19115:2003 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testUnmarshalling")
    public void testUnmarshallingLegacy() throws JAXBException {
        testUnmarshalling(Format.XML2007);
    }

    /**
     * Tests XML unmarshalling for a metadata version.
     * The version is not specified since it should be detected automatically.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     */
    private void testUnmarshalling(final Format format) throws JAXBException {
        final DefaultCitation c = unmarshalFile(DefaultCitation.class, openTestFile(format));
        assertTitleEquals("title", "Fight against poverty", c);

        final CitationDate date = getSingleton(c.getDates());
        assertEquals("date", date.getDate(), TestUtilities.date("2015-10-17 00:00:00"));
        assertEquals("dateType", DateType.valueOf("adopted"), date.getDateType());
        assertEquals("presentationForm", PresentationForm.valueOf("physicalObject"), getSingleton(c.getPresentationForms()));

        final Iterator<ResponsibleParty> it = c.getCitedResponsibleParties().iterator();
        final Contact contact = assertResponsibilityEquals(Role.ORIGINATOR, "Maid Marian", it.next());
        assertEquals("Contact instruction", "Send carrier pigeon.", String.valueOf(contact.getContactInstructions()));

        final OnlineResource resource = contact.getOnlineResource();
        assertEquals("Resource name", "IP over Avian Carriers", String.valueOf(resource.getName()));
        assertEquals("Resource description", "High delay, low throughput, and low altitude service.", String.valueOf(resource.getDescription()));
        assertEquals("Resource linkage", "https://tools.ietf.org/html/rfc1149", String.valueOf(resource.getLinkage()));
        assertEquals("Resource function", OnLineFunction.OFFLINE_ACCESS, resource.getFunction());

        // Thanks to xlink:href, the Contact shall be the same instance as above.
        assertSame("contact", contact, assertResponsibilityEquals(Role.valueOf("funder"), "Robin Hood", it.next()));
        assertFalse(it.hasNext());
    }

    /**
     * Asserts that the given responsibility has the expected properties, then returns its contact info.
     */
    private static Contact assertResponsibilityEquals(final Role role, final String name, final ResponsibleParty actual) {
        assertEquals("role", role, actual.getRole());
        final AbstractParty p = getSingleton(((DefaultResponsibleParty) actual).getParties());
        assertEquals("name", name, p.getName().toString());
        return getSingleton(p.getContactInfo());
    }
}
