package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringApplication
import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.Utils
import no.nav.legeerklaering.newInstance
import no.nav.legeerklaering.validation.Outcome
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Assert
import org.junit.Test
import java.util.*
import javax.xml.datatype.DatatypeFactory


class ArenaClientTest{

    val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")
    val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring
    val outcomeTypes = listOf(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, OutcomeType.BARN_AV_PASIENT)
    val tssid = "12454"
    val request = ArenaClient().createArenaEiaInfo(legeerklaring, fellesformat,0,outcomeTypes, tssid)

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
        Assert.assertEquals(LegeerklaeringApplication().getHCPFodselsnummer(fellesformat), request.legeData.fnr)
    }

    @Test
    fun shouldSetLegeDataTssid() {
        Assert.assertEquals(tssid, request.legeData.tssid)
    }

    @Test
    fun shouldSetEiaDataSystemSvar() {
        Assert.assertEquals(outcomeTypes[0].messageNumber.toBigInteger(), request.eiaData.systemSvar[0].meldingsNr)
        Assert.assertEquals(outcomeTypes[0].messageText, request.eiaData.systemSvar[0].meldingsTekst)
        Assert.assertEquals(outcomeTypes[0].messagePriority.priorityNumber.toBigInteger(), request.eiaData.systemSvar[0].meldingsPrioritet)
        Assert.assertEquals(outcomeTypes[0].messageType.toString(), request.eiaData.systemSvar[0].meldingsType)

        Assert.assertEquals(outcomeTypes[1].messageNumber.toBigInteger(), request.eiaData.systemSvar[1].meldingsNr)
        Assert.assertEquals(outcomeTypes[1].messageText, request.eiaData.systemSvar[1].meldingsTekst)
        Assert.assertEquals(outcomeTypes[1].messagePriority.priorityNumber.toBigInteger(), request.eiaData.systemSvar[1].meldingsPrioritet)
        Assert.assertEquals(outcomeTypes[1].messageType.toString(), request.eiaData.systemSvar[1].meldingsType)
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
