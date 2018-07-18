package no.nav.pale

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.mapping.ApprecError
import no.nav.pale.utils.shouldContainApprecError
import no.nav.pale.utils.shouldContainOutcome
import no.nav.pale.validation.OutcomeType
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.amshove.kluent.shouldBeNull
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.ZonedDateTime

class PaleReceiptIT : Spek({
    val e = EmbeddedEnvironment()

    afterEachTest { e.resetMocks() }
    afterGroup { e.shutdown() }

    describe("Valid message") {
        val person = defaultPerson()

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person))
        it("Creates outcome for valid messages") {
            e.readAppRec()
            val arenaEiaInfo = e.readArenaEiaInfo()
            // TODO: Check the receipt
            arenaEiaInfo shouldContainOutcome OutcomeType.LEGEERKLAERING_MOTTAT
        }
    }
    describe("Duplicate message") {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)

        e.defaultMocks(person)
        e.produceMessage(fellesformat)
        it("Creates valid receipt and ArenaEiaInfo") {
            e.readAppRec()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.LEGEERKLAERING_MOTTAT
        }
        e.produceMessage(fellesformat)
        it("Creates error receipt saying its a duplicate") {
            e.readAppRec() shouldContainApprecError ApprecError.DUPLICATE
            e.readArenaEiaInfo().shouldBeNull()
        }
    }
    describe("TPS returns person not found") {
        `when`(e.personV3Mock.hentPerson(any()))
                .thenThrow(HentPersonPersonIkkeFunnet("Person ikke funnet", PersonIkkeFunnet()))
        e.produceMessage(defaultFellesformat(person = defaultPerson()))
        it("Creates error receipt for missing person it population register") {
            e.readAppRec() shouldContainApprecError ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER
        }
    }
    describe("Missing patients first name in schema") {
        val person = defaultPerson().withPersonnavn(Personnavn().withEtternavn("Pasientsen"))

        e.produceMessage(defaultFellesformat(person = person))
        it("Creates error receipt for missing patient first name") {
            e.readAppRec() shouldContainApprecError ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA
        }
    }
    describe("Missing patients last name in schema") {
        val person = defaultPerson().withPersonnavn(Personnavn().withFornavn("Pasient"))

        e.produceMessage(defaultFellesformat(person = person))
        it("Creates error receipt for missing surname") {
            e.readAppRec() shouldContainApprecError ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA
        }
    }
    describe("Missing patient person number") {
        val person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent()))

        e.produceMessage(defaultFellesformat(person = person))
        it("Creates error receipt for misssing patient person number") {
            e.readAppRec() shouldContainApprecError ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA
        }
    }
    describe("Invalid patient person number") {
        val person = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678912")))

        e.produceMessage(defaultFellesformat(person = person))
        it("Creates error receipt for invalid person number") {
            e.readAppRec() shouldContainApprecError ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG
        }
    }
    describe("Missing doctor person number") {
        val doctor = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("")))

        e.produceMessage(defaultFellesformat(defaultPerson(), doctor = doctor))
        it("Creates error receipt for missing doctor person number") {
            e.readAppRec() shouldContainApprecError ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID
        }
    }
    describe("Invalid doctor person number") {
        val doctor = defaultPerson().withAktoer(PersonIdent().withIdent(NorskIdent().withIdent("12345678912")))

        e.produceMessage(defaultFellesformat(defaultPerson(), doctor = doctor))
        it("Creates error receipt for invalid person number") {
            e.readAppRec() shouldContainApprecError ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID
        }
    }
    describe("Message signed with a later date then received") {
        e.produceMessage(defaultFellesformat(defaultPerson(), signatureDate = ZonedDateTime.now().plusDays(1)))
        it("Creates error receipt for invalid signature date") {
            e.readAppRec() shouldContainApprecError ApprecError.GEN_DATE_ERROR
        }
    }
    describe("No NAV office in TPS") {
        val person = defaultPerson()

        e.defaultMocks(person, navOffice = null)
        e.produceMessage(defaultFellesformat(person))
        it("Creates error receipt for missing patient info") {
            e.readAppRec() shouldContainApprecError ApprecError.MISSING_PATIENT_INFO
        }
    }
    describe("Temporary downtime") {
        val person = defaultPerson()

        e.defaultMocks(person)
        `when`(e.personV3Mock.hentPerson(any()))
                .then {
                    Thread.sleep(5000)
                    HentPersonResponse().withPerson(person)
                }
                .thenReturn(HentPersonResponse().withPerson(person))
        e.produceMessage(defaultFellesformat(person))

        verify(e.personV3Mock, timeout(5000).atLeast(2)).hentPerson(any())
        it("Still handles the message") {
            val apprec = e.readAppRec()
            val arenaEiaInfo = e.readArenaEiaInfo()
            arenaEiaInfo shouldContainOutcome OutcomeType.LEGEERKLAERING_MOTTAT
        }
    }
})
