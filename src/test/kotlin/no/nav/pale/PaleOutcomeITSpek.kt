package no.nav.pale

import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.timeout
import no.nav.model.msghead.Ident
import no.nav.model.msghead.MsgHeadCV
import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.datagen.ident
import no.nav.pale.mapping.ApprecError
import no.nav.pale.model.Behandlingsvedlegg
import no.nav.pale.utils.readResourceAsString
import no.nav.pale.utils.shouldContainApprecError
import no.nav.pale.utils.shouldContainOutcome
import no.nav.pale.utils.shouldHaveOkStatus
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.extractBornDate
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.mockito.Mockito.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.GregorianCalendar

object PaleOutcomeITSpek : Spek({
    val e = EmbeddedEnvironment()
    beforeGroup { e.start() }
    afterEachTest {
        e.resetMocks()
    }
    afterGroup {
        e.shutdown()
    }
    describe("Full flow exception") {
        it("Ends on backout queue") {
            val message = readResourceAsString("/legeerklaering.xml")
            e.produceMessage(message)
            val messageOnBoq = e.consumeMessage(e.backoutConsumer)
            message shouldEqual messageOnBoq
        }
    }
    describe("Patient over 70 years old") {
        it("Creates error receipt with outcome for being over 70 years old") {
            val person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE))
            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person))
            e.readAppRec().shouldContainApprecError(apprecError = ApprecError.PATIENT_IS_OVER_70)
        }
    }
    describe("Sperrekode 6") {
        it("Creates no ArenaEiaInfo") {
            val person = defaultPerson().apply {
                diskresjonskode = Diskresjonskoder().apply {
                    value = "SPSF"
                }
            }
            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person))
            e.readAppRec().shouldNotBeNull()
            e.consumeMessage(e.arenaConsumer).shouldBeNull()
        }
    }
    describe("Sperrekode 7") {
        it("Creates ArenaEiaInfo") {
            val person = defaultPerson().apply {
                diskresjonskode = Diskresjonskoder().apply {
                    value = "SPFO"
                }
            }
            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person))
            e.readAppRec().shouldHaveOkStatus()
            val arenaEiaInfo = e.readArenaEiaInfo()
            arenaEiaInfo.shouldNotBeNull()
            arenaEiaInfo.pasientData.spesreg shouldEqual 7
        }
    }
    describe("Patient married to the doctor") {
        it("Creates outcome for being married to patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.EKTEFELLE.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.MARRIED_TO_PATIENT
        }
    }
    describe("Patient registered partner with doctor") {
        it("Creates outcome for being a registered partner with patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.REGISTRERT_PARTNER_MED.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.REGISTERED_PARTNER_WITH_PATIENT
        }
    }
    describe("Doctor is patients father") {
        it("Creates outcome for being parent to patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.FAR.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.PARENT_TO_PATIENT
        }
    }
    describe("Doctor is patients foster father") {
        it("Creates outcome for being parent to patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.FOSTERFAR.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.PARENT_TO_PATIENT
        }
    }
    describe("Doctor is patients mother") {
        it("Creates outcome for being parent to patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.MOR.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.PARENT_TO_PATIENT
        }
    }
    describe("Doctor is patients foster mother") {
        it("Creates outcome for being parent to patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.FOSTERMOR.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.PARENT_TO_PATIENT
        }
    }
    describe("Doctor is patients child") {
        it("Creates outcome for being child of patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.BARN.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.CHILD_OF_PATIENT
        }
    }
    describe("Doctor is patients foster child") {
        it("Creates outcome for being child of patient") {
            val doctor = defaultPerson()
            val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                    .withTilRolle(Familierelasjoner().withValue(RelationType.FOSTERBARN.kodeverkVerdi)))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person = person, doctor = doctor))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.CHILD_OF_PATIENT
        }
    }
    describe("Behandler is not in SAR") {
        it("Creates outcome for doctor not being in SAR") {
            val person = defaultPerson()

            e.defaultMocks(person, doctor = null)
            e.produceMessage(defaultFellesformat(person))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.BEHANDLER_NOT_SAR
        }
    }
    describe("Patient is registered as emigrated") {
        it("Creates outcome for being emigrated") {
            val person = defaultPerson().apply {
                personstatus = Personstatus().withPersonstatus(Personstatuser().withValue("UTVA"))
            }

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.PATIENT_EMIGRATED
        }
    }
    describe("Doctor has person number in SAR but uses DNR") {
        it("Creates outcome for having DNR in schema but valid FNR in SAR") {
            val person = defaultPerson()
            val doctor = defaultPerson()

            val fellesformat = defaultFellesformat(person, doctor = doctor)
            val dnr = generatePersonNumber(extractBornDate(person.ident()), useDNumber = true)
            fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur = dnr
            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.clear()
            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.add(Ident().apply {
                typeId = MsgHeadCV().apply {
                    v = "DNR"
                }
                id = dnr
            })

            e.defaultMocks(person)
            e.produceMessage(fellesformat)

            argumentCaptor<String>().apply {
                verify(e.pdfGenMock, timeout(1000).times(2)).getPDF(capture())
                val json = objectMapper.readValue(allValues[1], Behandlingsvedlegg::class.java)
                json.sender.merknadNotis.map { it.number } shouldContain OutcomeType.BEHANDLER_HAS_FNR_USES_DNR.messageNumber
            }

            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo().shouldNotBeNull()
        }
    }
    describe("Doctor is the patient") {
        it("Creates outcome for doctor being the patient") {
            val doctor = defaultPerson()

            e.defaultMocks(doctor, doctor = doctor)
            e.produceMessage(defaultFellesformat(doctor, doctor = doctor))
            e.readAppRec().shouldContainApprecError(apprecError = ApprecError.BEHANDLER_IS_PATIENT)
        }
    }
    describe("Patient is registered dead in TPS") {
        it("Creates outcome for being registered as dead in TPS") {
            val person = defaultPerson()
                    .withDoedsdato(Doedsdato().withDoedsdato(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())))

            e.defaultMocks(person)
            e.produceMessage(defaultFellesformat(person))
            e.readAppRec().shouldHaveOkStatus()
            e.readArenaEiaInfo() shouldContainOutcome OutcomeType.REGISTERED_DEAD_IN_TPS
        }
    }
})
