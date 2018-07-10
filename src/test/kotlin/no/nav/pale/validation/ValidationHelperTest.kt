package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.RelationType
import no.nav.pale.utils.readToFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ValidationHelperTest {

    private val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
    private val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)
    private val fellesformatWithDNr: EIFellesformat = readToFellesformat("/validation/legeerklaeringWithDNR.xml")

    @Test
    fun shouldExtractDoctorIdentFromSenderFNR() {
        val fnr = extractDoctorIdentFromSender(fellesformat)
        assertEquals("04030350265", fnr?.id)
    }

    @Test
    fun shouldExtractDoctorIdentFromSenderDNR() {
        val dnr = extractDoctorIdentFromSender(fellesformatWithDNr)
        assertEquals("45069800525", dnr?.id)
    }

    @Test
    fun shouldExtractDoctorIdentFromSignature() {
        val fnr = extractDoctorIdentFromSignature(fellesformat)
        assertEquals("04030350265", fnr)
    }

    @Test
    fun testPre500IndividualNumberCauses19xxBornDate() {
        // We're using the first possible valid person number for the date 1.1.1905
        assertEquals(extractBornDate("03110511220").year, 1905)
    }

    @Test
    fun shouldExtractSenderOrganisationName() {
        val organisationName = extractSenderOrganisationName(fellesformat)
        assertEquals("Kule helsetjenester AS", organisationName)
    }

    @Test
    fun shouldExtractPersonIdent() {
        val patientFnr = extractPersonIdent(legeerklaring)
        assertEquals("12128913767", patientFnr)
    }

    @Test
    fun shouldExtractPatientSurname() {
        val patientSurname = extractPatientSurname(legeerklaring)
        assertEquals("Bergheim", patientSurname)
    }

    @Test
    fun shouldExtractPatientFirstname() {
        val patientFirst = extractPatientFirstName(legeerklaring)
        assertEquals("Daniel", patientFirst)
    }

    @Test
    fun shouldExtractBornDateFNR() {
        val patientBornDate = extractBornDate("12128913767")
        assertEquals(LocalDate.of(1989, 12, 12), patientBornDate)
    }

    @Test
    fun shouldExtractBornDateDNR() {
        val patientBornDate = extractBornDate("45069800525")
        assertEquals(LocalDate.of(1998, 6, 5), patientBornDate)
    }

    @Test
    fun shouldfindDoctorInRelationsToPatientEktefelle() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("EKTE")
            harFraRolleI.add(familierelasjon)
        }

        val doctorPersonnumber = extractDoctorIdentFromSender(fellesformat)?.id!!
        val familierelasjon = findDoctorInRelations(patient, doctorPersonnumber)!!

        assertEquals(RelationType.fromKodeverkValue(familierelasjon.tilRolle.value)?.kodeverkVerdi, RelationType.EKTEFELLE.kodeverkVerdi)
    }

    @Test
    fun shouldExtractSignatureDate() {

        val signatureDate = extractSignatureDate(fellesformat)
        val expectetsignatureDate = LocalDate.of(2017, 12, 29)

        assertEquals(expectetsignatureDate.year, signatureDate.year)
        assertEquals(expectetsignatureDate.month, signatureDate.month)
        assertEquals(expectetsignatureDate.dayOfMonth, signatureDate.dayOfMonth)
    }

    @Test
    fun shouldExtractCompanyNumberFromSender() {
        val organisationNumberFromSender = extractOrganisationNumberFromSender(fellesformat)

        assertEquals("223456789", organisationNumberFromSender?.id)
    }

    fun familierelasjon(faimilierelasjon: String): Familierelasjon = Familierelasjon().apply {
        tilRolle = Familierelasjoner().apply {
            value = faimilierelasjon
        }
        tilPerson = Person().apply {

            aktoer = PersonIdent().apply {
                ident = NorskIdent().apply {
                    val doctorIdent = extractDoctorIdentFromSender(fellesformat)!!
                    ident = doctorIdent.id
                    type = Personidenter().apply {
                        value = doctorIdent.typeId.v
                    }
                }
            }
        }
    }
}
