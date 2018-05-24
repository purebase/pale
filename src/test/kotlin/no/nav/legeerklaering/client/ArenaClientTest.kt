package no.nav.legeerklaering.client

import no.nav.legeerklaering.*
import no.nav.legeerklaering.mapping.ApprecError
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.legeerklaering.validation.extractLegeerklaering
import no.nav.legeerklaering.validation.toOutcome
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.util.*



class ArenaClientTest{

    private val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
    private val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)
    private val outcomes = listOf(
            OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.toOutcome(generatePersonNumber(LocalDate.now().minusYears(40)), apprecError = ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA),
            OutcomeType.CHILD_OF_PATIENT.toOutcome()
    )
    private val tssid = "12454"
    private val request: ArenaEiaInfo = createArenaEiaInfo(fellesformat, outcomes, tssid)

    @Test
    fun shouldSetEdiloggId() {
        Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId, request.ediloggId)
    }

    @Test
    fun shouldSethendelseStatus() {
        Assert.assertEquals(LegeerklaeringConstant.tilvurdering.string, request.hendelseStatus)
    }

    @Test
    fun shouldSetVersion() {
        Assert.assertEquals("2.0", request.version)
    }

    @Test
    fun shouldSetSkjemaTypen() {
        Assert.assertEquals(LegeerklaeringConstant.LE.string, request.skjemaType)
    }

    @Test
    fun shouldSetMappeType() {
        Assert.assertEquals(LegeerklaeringConstant.mappetypeRP.string, request.mappeType)
    }

    @Test
    fun shouldSetPasientDataFnr() {
        Assert.assertEquals(legeerklaring.pasientopplysninger.pasient.fodselsnummer, request.pasientData.fnr)
    }

    @Test
    fun shouldSetPasientDataSperret() {
        Assert.assertEquals(false, request.pasientData.isSperret)
    }

    @Test
    fun shouldSetPasientDataTkNummer() {
        Assert.assertEquals("", request.pasientData.tkNummer)
    }

    @Test
    fun shouldSetLegeDataNavn() {
        Assert.assertEquals(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName.toUpperCase()+ " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName.toUpperCase() + " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName.toUpperCase(), request.legeData.navn)
    }

    @Test
    fun shouldSetLegeDataFnr() {
        Assert.assertEquals(getHCPFodselsnummer(fellesformat), request.legeData.fnr)
    }

    @Test
    fun shouldSetLegeDataTssid() {
        Assert.assertEquals(tssid, request.legeData.tssid)
    }

    @Test
    fun shouldSetEiaDataSignaturDato() {

        val expectedCurrentDate = newInstance.newXMLGregorianCalendar(GregorianCalendar().apply {
            set(fellesformat.msgHead.msgInfo.genDate.year,
                    fellesformat.msgHead.msgInfo.genDate.month,fellesformat.msgHead.msgInfo.genDate.day)
        })

        Assert.assertEquals(expectedCurrentDate.day, request.eiaData.signaturDato.day)
        Assert.assertEquals(expectedCurrentDate.hour, request.eiaData.signaturDato.hour)
        Assert.assertEquals(expectedCurrentDate.minute, request.eiaData.signaturDato.minute)

    }

}
