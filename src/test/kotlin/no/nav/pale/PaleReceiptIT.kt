package no.nav.pale

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.mapping.ApprecError
import no.nav.pale.utils.assertArenaInfoContains
import no.nav.pale.validation.OutcomeType
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime
import javax.xml.datatype.DatatypeFactory

class PaleReceiptIT {
    private val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

    @Before
    fun resetMocks() {
        e.resetMocks()
    }

    @Test
    fun testValidMessageProducesOkReceipt() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.defaultMocks(person)

        e.produceMessage(fellesformatString)

        e.readAppRec()
        val arenaEiaInfo = e.readArenaEiaInfo()
        // TODO: Check the receipt
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.LEGEERKLAERING_MOTTAT)
    }

    @Test
    fun testDuplicateReceipt() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.defaultMocks(person)

        e.produceMessage(fellesformatString)

        e.readAppRec()
        e.readArenaEiaInfo()

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()

        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.DUPLICATE.s, apprec.error[0].s)
        assertEquals(ApprecError.DUPLICATE.v, apprec.error[0].v)
    }

    @Test
    fun testTPSReturnsMissingReceipt() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        `when`(e.personV3Mock.hentPerson(any())).thenThrow(HentPersonPersonIkkeFunnet("Person ikke funnet", PersonIkkeFunnet()))

        val appRec = e.readAppRec()

        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER.s, appRec.error[0].s)
    }

    @Test
    fun testMissingPatientFirstnameMissingReceipt() {
        val person = defaultPerson().apply {
            personnavn.fornavn = null
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA.v, apprec.error[0].v)
    }

    @Test
    fun testMissingPatientSurnameMissingReceipt() {
        val person = defaultPerson().apply {
            personnavn.etternavn = null
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA.v, apprec.error[0].v)
    }

    @Test
    fun testMissingPersonNumberReceipt() {
        val person = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = ""
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA.v, apprec.error[0].v)
    }

    @Test
    fun testInvalidPersonNumberReceipt() {
        val person = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = "12345678912"
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.v, apprec.error[0].v)
    }

    @Test
    fun testMissingDoctorPersonNumberReceipt() {
        val doctor = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = ""
        }

        val fellesformat = defaultFellesformat(defaultPerson(), doctor = doctor)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.s, apprec.error[0].s)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v, apprec.error[0].v)
    }

    @Test
    fun testInvalidDoctorPersonNumberReceipt() {
        val doctor = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = "12345678912"
        }

        val fellesformat = defaultFellesformat(defaultPerson(), doctor = doctor)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.s, apprec.error[0].s)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v, apprec.error[0].v)
    }

    @Test
    fun testInvalidGenDateReceipt() {
        val fellesformat = defaultFellesformat(defaultPerson(), signatureDate = ZonedDateTime.now().plusDays(1))
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.GEN_DATE_ERROR.s, apprec.error[0].s)
        assertEquals(ApprecError.GEN_DATE_ERROR.v, apprec.error[0].v)
    }

    @Test
    fun testNoNavOfficeCausesMissingPatientInfoReceipt() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        e.produceMessage(fellesformatString)

        e.defaultMocks(person, navOffice = null)

        val apprec = e.readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.MISSING_PATIENT_INFO.s, apprec.error[0].s)
        assertEquals(ApprecError.MISSING_PATIENT_INFO.v, apprec.error[0].v)
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
