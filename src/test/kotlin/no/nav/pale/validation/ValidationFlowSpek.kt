package no.nav.pale.validation

import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.utils.shouldContainOutcome
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar

object ValidationFlowSpek : Spek({
    describe("Patients person number missing") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent()))
        )
        it("Creates outcome for missing person number") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND
        }
    }
    describe("Patients person number is empty") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("")))
                        .withPersonnavn(Personnavn().withFornavn("Pasient").withEtternavn("Pasientsen"))
        )
        it("Creates outcome for missing person number") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND
        }
    }
    describe("Patients person number is not 11 characters") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson()
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345")))
                        .withPersonnavn(Personnavn().withFornavn("Pasient").withEtternavn("Pasientsen"))
        )
        it("Creates outcome for person number not being 11 characters") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.PERSON_NUMBER_NOT_11_DIGITS
        }
        it("Creates the expected formatted message") {
            validationFlow(fellesformat)[0].formattedMessage shouldBeEqualTo
                    "Pasientsen Pasient sitt fødselsnummer eller D-nummer 12345 er ikke 11 tegn. Det er 5 tegn langt."
        }
    }
    describe("Doctors person number is not 11 characters") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = defaultPerson()
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("654321")))
                        .withPersonnavn(Personnavn().withFornavn("Lege").withEtternavn("Legesen"))
        )
        it("Creates outcome for person number not being 11 characters") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.PERSON_NUMBER_NOT_11_DIGITS
        }
        it("Creates the expected formatted message") {
            validationFlow(fellesformat)[0].formattedMessage shouldBeEqualTo
                    "LEGESEN LEGE sitt fødselsnummer eller D-nummer 654321 er ikke 11 tegn. Det er 6 tegn langt."
        }
    }
    describe("Patients person number is invalid") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678912")))
                        .withPersonnavn(Personnavn().withFornavn("Pasient").withEtternavn("Pasientsen")),
                doctor = defaultPerson()
        )
        it("Creates outcome for invalid person number") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER
        }
        it("Creates the expected formatted message") {
            validationFlow(fellesformat)[0].formattedMessage shouldBeEqualTo
                    "Fødselsnummeret eller D-nummeret 12345678912 til Pasientsen Pasient er feil."
        }
    }
    describe("Doctors person number is invalid") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678913")))
                        .withPersonnavn(Personnavn().withFornavn("Lege").withEtternavn("Legesen"))
        )
        it("Creates outcome for invalid person number") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER
        }
        it("Creates the expected formatted message") {
            validationFlow(fellesformat)[0].formattedMessage shouldBeEqualTo
                    "Fødselsnummeret eller D-nummeret 12345678913 til LEGESEN LEGE er feil."
        }
    }
    describe("Patient missing surname") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withPersonnavn(Personnavn().withFornavn("Nosurname"))
        )
        it("Creates outcome for missing patient surname") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.PATIENT_SURNAME_NOT_FOUND
        }
    }
    describe("Patient missing first name") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson().withPersonnavn(Personnavn().withEtternavn("Nofirstname"))
        )
        it("Creates outcome for missing patient first name") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND
        }
    }
    describe("Mismatched person number in schema and signature") {
        val fellesformat = defaultFellesformat(person = defaultPerson())
        fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur = generatePersonNumber(LocalDate.now())
        it("Creates outcome for mismatched person number in signature and schema") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA
        }
    }
    describe("Signature date is newer then received date") {
        val received = ZonedDateTime.of(2018, 5, 4, 12, 0, 0, 0, ZoneId.systemDefault())
        val signed = ZonedDateTime.of(2020, 5, 4, 12, 0, 0, 0, ZoneId.systemDefault())

        val fellesformat = defaultFellesformat(person = defaultPerson()).apply {
            msgHead.msgInfo.genDate = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(signed))
            mottakenhetBlokk.mottattDatotid = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(received))
        }
        it("Creates outcome for too new signature") {
            validationFlow(fellesformat) shouldContainOutcome OutcomeType.SIGNATURE_TOO_NEW
        }
        it("Creates the expected formatted message") {
            validationFlow(fellesformat)[0].formattedMessage shouldBeEqualTo
                    "Melding mottatt til behandling i dag 04.05.2018 12:00:00 er signert med dato 04.05.2020 12:00:00, og avvises"
        }
    }
})
