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
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

class ValidationHelperTest {

    private val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
    private val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)
    private val fellesformatWithDNr: EIFellesformat = readToFellesformat("/validation/legeerklaeringWithDNR.xml")

    @Test
    fun shouldExtractDoctorIdentFromSenderFNR() {
        val fnr = extractDoctorIdentFromSender(fellesformat)
        Assert.assertEquals("04030350265", fnr?.id)
    }

    @Test
    fun shouldExtractDoctorIdentFromSenderDNR() {
        val dnr = extractDoctorIdentFromSender(fellesformatWithDNr)
        Assert.assertEquals("45069800525", dnr?.id)
    }

    @Test
    fun shouldExtractDoctorIdentFromSignature() {
        val fnr = extractDoctorIdentFromSignature(fellesformat)
        Assert.assertEquals("04030350265", fnr)
    }

    @Test
    fun shouldExtractSenderOrganisationName() {
        val organisationName = extractSenderOrganisationName(fellesformat)
        Assert.assertEquals("Kule helsetjenester AS", organisationName)
    }

    @Test
    fun shouldExtractPersonIdent() {
        val patientFnr = extractPersonIdent(legeerklaring)
        Assert.assertEquals("12128913767", patientFnr)
    }

    @Test
    fun shouldExtractPatientSurname() {
        val patientSurname = extractPatientSurname(legeerklaring)
        Assert.assertEquals("Bergheim", patientSurname)
    }

    @Test
    fun shouldExtractPatientFirstname() {
        val patientFirst = extractPatientFirstName(legeerklaring)
        Assert.assertEquals("Daniel", patientFirst)
    }

    @Test
    fun shouldExtractBornDateFNR() {
        val patientBornDate = extractBornDate("12128913767")
        Assert.assertEquals(LocalDate.of(2089,12,12), patientBornDate)
    }

    @Test
    fun shouldExtractBornDateDNR() {
        val patientBornDate = extractBornDate("45069800525")
        Assert.assertEquals(LocalDate.of(2098,6,15), patientBornDate)
    }

    @Test
    fun shouldfindDoctorInRelationsToPatientEktefelle() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("EKTE")
            harFraRolleI.add(familierelasjon)
        }

        val doctorPersonnumber = extractDoctorIdentFromSender(fellesformat)?.id!!
        val familierelasjon = findDoctorInRelations(patient, doctorPersonnumber)!!

        Assert.assertEquals(RelationType.fromKodeverkValue(familierelasjon.tilRolle.value)?.kodeverkVerdi, RelationType.EKTEFELLE.kodeverkVerdi)
    }

    @Test
    fun shouldExtractSignatureDate() {

        val signatureDate = extractSignatureDate(fellesformat)
        val expectetsignatureDate = LocalDate.of(2017,12,29)

        Assert.assertEquals(expectetsignatureDate.year, signatureDate.year)
        Assert.assertEquals(expectetsignatureDate.month, signatureDate.month)
        Assert.assertEquals(expectetsignatureDate.dayOfMonth, signatureDate.dayOfMonth)
    }

    @Test
    fun shouldExtractCompanyNumberFromSender() {

        val organisationNumberFromSender = extractOrganisationNumberFromSender(fellesformat)

        Assert.assertEquals("223456789",organisationNumberFromSender?.id)

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