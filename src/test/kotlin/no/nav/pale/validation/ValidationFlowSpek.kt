package no.nav.pale.validation

import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.utils.assertOutcomesContain
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar

class ValidationFlowTest {

    @Test
    fun testCreatesPatientNumberNotFoundOnNotFound() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent()))
        )
        assertOutcomesContain(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesPatientNumberNotFoundOnEmpty() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("")))
        )
        assertOutcomesContain(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesPersonNumberNot11DigitsOnPatient() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345")))
        )
        assertOutcomesContain(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesPersonNumberNot11DigitsOnDoctor() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345")))
        )
        assertOutcomesContain(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesInvalidPersonNumberOutcomeOnPatient() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678912"))),
                doctor = defaultPerson()
        )
        assertOutcomesContain(OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesInvalidPersonNumberOutcomeOnDoctor() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678913")))
        )
        assertOutcomesContain(OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesMissingPatientSurnameOutcome() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withPersonnavn(Personnavn().withFornavn("Nosurname"))
        )
        assertOutcomesContain(OutcomeType.PATIENT_SURNAME_NOT_FOUND, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesMissingPatientFirstNameOutcome() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withPersonnavn(Personnavn().withEtternavn("Nofirstname"))
        )
        assertOutcomesContain(OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesMismatchedPersonNumberOutcome() {
        val fellesformat = defaultFellesformat(person = defaultPerson())
        fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur = generatePersonNumber(LocalDate.now())

        assertOutcomesContain(OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA, validationFlow(fellesformat))
    }

    @Test
    fun testCreatesSignatureTooNewOutcome() {
        val received = ZonedDateTime.of(2018, 5, 4, 12, 0, 0, 0, ZoneId.systemDefault())
        val signed = ZonedDateTime.of(2020, 5, 4, 12, 0, 0, 0, ZoneId.systemDefault())

        val fellesformat = defaultFellesformat(person = defaultPerson()).apply {
            msgHead.msgInfo.genDate = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(signed))
            mottakenhetBlokk.mottattDatotid = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(received))
        }
        assertOutcomesContain(OutcomeType.SIGNATURE_TOO_NEW, validationFlow(fellesformat))
    }

    @Test
    fun testPatientPersonNumberNot11DigitsFormattedText() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson()
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345")))
                        .withPersonnavn(Personnavn().withFornavn("Pasient").withEtternavn("Pasientsen"))
        )
        assertEquals("Pasientsen Pasient sitt fødselsnummer eller D-nummer 12345 er ikke 11 tegn. Det er 5 tegn langt.",
                validationFlow(fellesformat)[0].formattedMessage)
    }

    @Test
    fun testDoctorPersonNumberNot11DigitsFormattedText() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = defaultPerson()
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("654321")))
                        .withPersonnavn(Personnavn().withFornavn("Lege").withEtternavn("Legesen"))
        )
        assertEquals("LEGESEN LEGE sitt fødselsnummer eller D-nummer 654321 er ikke 11 tegn. Det er 6 tegn langt.",
                validationFlow(fellesformat)[0].formattedMessage)
    }

    @Test
    fun testDoctorPersonNumberInvalidFormattedText() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = defaultPerson()
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678913")))
                        .withPersonnavn(Personnavn().withFornavn("Lege").withEtternavn("Legesen"))
        )
        assertEquals("Fødselsnummeret eller D-nummeret 12345678913 til LEGESEN LEGE er feil.",
                validationFlow(fellesformat)[0].formattedMessage)
    }

    @Test
    fun testPatientPersonNumberInvalidFormattedText() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson()
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678912")))
                        .withPersonnavn(Personnavn().withFornavn("Pasient").withEtternavn("Pasientsen"))
        )
        assertEquals("Fødselsnummeret eller D-nummeret 12345678912 til Pasientsen Pasient er feil.",
                validationFlow(fellesformat)[0].formattedMessage)
    }

    @Test
    fun testCreatesSignatureTooNewFormattedText() {
        val received = ZonedDateTime.of(2018, 5, 4, 12, 0, 0, 0, ZoneId.systemDefault())
        val signed = ZonedDateTime.of(2020, 5, 4, 12, 0, 0, 0, ZoneId.systemDefault())

        val fellesformat = defaultFellesformat(person = defaultPerson()).apply {
            msgHead.msgInfo.genDate = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(signed))
            mottakenhetBlokk.mottattDatotid = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(received))
        }
        assertEquals("Melding mottatt til behandling i dag 04.05.2018 12:00:00 er signert med dato 04.05.2020 12:00:00, og avvises",
                validationFlow(fellesformat)[0].formattedMessage)
    }
}
