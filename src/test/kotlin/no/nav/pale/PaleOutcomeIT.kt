package no.nav.pale

import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.utils.assertArenaInfoContains
import no.nav.pale.validation.OutcomeType
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import org.apache.commons.io.IOUtils
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.reset
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths

class PaleOutcomeIT {
    @Before
    fun resetMocks() {
        reset(e.personV3Mock, e.organisasjonEnhetV2Mock)
    }

    fun readAppRec(): AppRec {
        val fellesformat = fellesformatJaxBContext.createUnmarshaller()
                .unmarshal(StringReader(e.consumeMessage(e.apprecConsumer))) as EIFellesformat
        return fellesformat.appRec
    }

    fun readArenaEiaInfo(): ArenaEiaInfo = arenaEiaInfoJaxBContext.createUnmarshaller()
                .unmarshal(StringReader(e.consumeMessage(e.arenaConsumer))) as ArenaEiaInfo

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
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.defaultMocks(person)

        e.produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.PATIENT_IS_OVER_70)
    }

    @Test
    fun testSperrekode6CausesNoArenaEiaInfo() {
        val person = defaultPerson().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPSF"
            }
        }
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.defaultMocks(person)

        e.produceMessage(fellesformatString)

        readAppRec()
        assertNull(e.consumeMessage(e.arenaConsumer))
    }

    @Test
    fun testSperreKode7CausesArenaMessage() {
        val person = defaultPerson().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPFO"
            }
        }
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.defaultMocks(person)

        e.produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertEquals(7, arenaEiaInfo.pasientData.spesreg)
    }

    @Test
    fun testPatientMarriedToDoctor() {
        val doctor = defaultPerson()
        val person = defaultPerson().apply {
            this.harFraRolleI.add(
                    Familierelasjon().withTilPerson(doctor)
                            .withTilRolle(Familierelasjoner()
                            .withValue(RelationType.EKTEFELLE.kodeverkVerdi))
            )
        }
        val fellesformat = defaultFellesformat(person = person, doctor = doctor)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.defaultMocks(person)

        e.produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.MARRIED_TO_PATIENT)
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
