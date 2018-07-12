package no.nav.pale

import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.model.msghead.Ident
import no.nav.model.msghead.MsgHeadCV
import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.datagen.ident
import no.nav.pale.utils.assertArenaInfoContains
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.extractBornDate
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.GregorianCalendar

class PaleOutcomeIT {
    @After
    fun resetMocks() {
        e.resetMocks()
    }

    @Test
    fun testFullFlowExceptionSendMessageToBOQ() {
        e.produceMessage(IOUtils.toString(PaleOutcomeIT::class.java.getResourceAsStream("/legeerklaering.xml"), Charsets.ISO_8859_1))

        val messageOnBoq = e.consumeMessage(e.backoutConsumer)

        assertEquals("Should be",
                String(Files.readAllBytes(Paths.get(
                        PaleOutcomeIT::class.java.getResource("/legeerklaering.xml").toURI())), Charsets.ISO_8859_1),
                messageOnBoq)
    }

    @Test
    fun testPersonOver70() {
        val person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.PATIENT_IS_OVER_70)
    }

    @Test
    fun testSperrekode6CausesNoArenaEiaInfo() {
        val person = defaultPerson().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPSF"
            }
        }

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person))

        e.readAppRec()
        assertNull(e.consumeMessage(e.arenaConsumer))
    }

    @Test
    fun testSperreKode7CausesArenaMessage() {
        val person = defaultPerson().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPFO"
            }
        }

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertEquals(7, arenaEiaInfo.pasientData.spesreg)
    }

    @Test
    fun testPatientMarriedToDoctor() {
        val doctor = defaultPerson()
        val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                .withTilRolle(Familierelasjoner().withValue(RelationType.EKTEFELLE.kodeverkVerdi)))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person, doctor = doctor))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.MARRIED_TO_PATIENT)
    }

    @Test
    fun testPatientRegisteredPartnerWithDoctor() {
        val doctor = defaultPerson()
        val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                .withTilRolle(Familierelasjoner().withValue(RelationType.REGISTRERT_PARTNER_MED.kodeverkVerdi)))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person, doctor = doctor))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.REGISTERED_PARTNER_WITH_PATIENT)
    }

    @Test
    fun testDoctorIsPatientsFather() {
        val doctor = defaultPerson()
        val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                .withTilRolle(Familierelasjoner().withValue(RelationType.FAR.kodeverkVerdi)))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person, doctor = doctor))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.PARENT_TO_PATIENT)
    }

    @Test
    fun testDoctorIsPatientsMother() {
        val doctor = defaultPerson()
        val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                .withTilRolle(Familierelasjoner().withValue(RelationType.MOR.kodeverkVerdi)))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person, doctor = doctor))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.PARENT_TO_PATIENT)
    }

    @Test
    fun testDoctorIsPatientsChild() {
        val doctor = defaultPerson()
        val person = defaultPerson().withHarFraRolleI(Familierelasjon().withTilPerson(doctor)
                .withTilRolle(Familierelasjoner().withValue(RelationType.BARN.kodeverkVerdi)))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person = person, doctor = doctor))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.CHILD_OF_PATIENT)
    }

    @Test
    fun testBehandlerNotInSar() {
        val person = defaultPerson()

        e.defaultMocks(person, doctor = null)
        e.produceMessage(defaultFellesformat(person))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.BEHANDLER_NOT_SAR)
    }

    @Test
    fun testPatientHasEmigrated() {
        val person = defaultPerson().apply {
            personstatus = Personstatus().withPersonstatus(Personstatuser().withValue("UTVA"))
        }

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.PATIENT_EMIGRATED)
    }

    @Ignore("The result of this test is expected, however it doesn't send anything to arena or joark, " +
            "do we need it?")
    @Test
    fun testDoctorHasPersonNumberInSarButUsesDNumber() {
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

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR)
    }

    @Test
    fun testDoctorIsThePatient() {
        val doctor = defaultPerson()

        e.defaultMocks(doctor, doctor = doctor)
        e.produceMessage(defaultFellesformat(doctor, doctor = doctor))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.BEHANDLER_IS_PATIENT)
    }

    @Test
    fun testPatientIsRegisteredAsDeadInTPS() {
        val person = defaultPerson()
                .withDoedsdato(Doedsdato().withDoedsdato(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())))

        e.defaultMocks(person)
        e.produceMessage(defaultFellesformat(person))

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.REGISTERED_DEAD_IN_TPS)
    }

    companion object {
        val e: EmbeddedEnvironment = EmbeddedEnvironment()

        @AfterClass
        @JvmStatic
        fun tearDown() {
            e.shutdown()
        }
    }
}
