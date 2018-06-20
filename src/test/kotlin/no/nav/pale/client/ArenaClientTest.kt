package no.nav.pale.client

import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.mapping.ApprecError
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.extractLegeerklaering
import no.nav.pale.validation.toOutcome
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.PaleConstant
import no.nav.pale.getHCPFodselsnummer
import no.nav.pale.newInstance
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate
import java.util.GregorianCalendar

class ArenaClientTest {

    private val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
    private val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)
    private val outcomes = listOf(
            OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.toOutcome(generatePersonNumber(LocalDate.now().minusYears(40)), apprecError = ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA),
            OutcomeType.CHILD_OF_PATIENT.toOutcome()
    )
    private val tssid = "12454"
    private val navkontor = "0301"
    private val request: ArenaEiaInfo = createArenaEiaInfo(fellesformat, outcomes, tssid, null, navkontor)

    @Test
    fun shouldSetEdiloggId() {
        Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId, request.ediloggId)
    }

    @Test
    fun shouldSethendelseStatus() {
        Assert.assertEquals(PaleConstant.tilvurdering.string, request.hendelseStatus)
    }

    @Test
    fun shouldSetVersion() {
        Assert.assertEquals("2.0", request.version)
    }

    @Test
    fun shouldSetSkjemaTypen() {
        Assert.assertEquals(PaleConstant.LE.string, request.skjemaType)
    }

    @Test
    fun shouldSetMappeType() {
        Assert.assertEquals(PaleConstant.mappetypeRP.string, request.mappeType)
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
        Assert.assertEquals(navkontor, request.pasientData.tkNummer)
    }

    @Test
    fun shouldSetLegeDataNavn() {
        Assert.assertEquals(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName.toUpperCase() + " " +
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

    // TODO se TODO sjekke med arena om dette benyttes vi tror ikkje det
    @Ignore
    @Test
    fun shouldSetEiaDataSystemSvar() {
        Assert.assertEquals(outcomes[0].outcomeType.messageNumber.toBigInteger(), request.eiaData.systemSvar[0].meldingsNr)
        Assert.assertEquals(outcomes[0].formattedMessage, request.eiaData.systemSvar[0].meldingsTekst)
        Assert.assertEquals(outcomes[0].outcomeType.messagePriority.priorityNumber.toBigInteger(), request.eiaData.systemSvar[0].meldingsPrioritet)
        Assert.assertEquals(outcomes[0].outcomeType.messageType.type, request.eiaData.systemSvar[0].meldingsType)

        Assert.assertEquals(outcomes[1].outcomeType.messageNumber.toBigInteger(), request.eiaData.systemSvar[1].meldingsNr)
        Assert.assertEquals(outcomes[1].formattedMessage, request.eiaData.systemSvar[1].meldingsTekst)
        Assert.assertEquals(outcomes[1].outcomeType.messagePriority.priorityNumber.toBigInteger(), request.eiaData.systemSvar[1].meldingsPrioritet)
        Assert.assertEquals(outcomes[1].outcomeType.messageType.type, request.eiaData.systemSvar[1].meldingsType)
    }

    // TODO se TODO sjekke med arena om dette benyttes vi tror ikkje det
    @Ignore
    @Test
    fun shouldSetEiaDataSignaturDato() {

        val expectedCurrentDate = newInstance.newXMLGregorianCalendar(GregorianCalendar().apply {
            set(fellesformat.msgHead.msgInfo.genDate.year,
                    fellesformat.msgHead.msgInfo.genDate.month, fellesformat.msgHead.msgInfo.genDate.day)
        })

        Assert.assertEquals(expectedCurrentDate.day, request.eiaData.signaturDato.day)
        Assert.assertEquals(expectedCurrentDate.hour, request.eiaData.signaturDato.hour)
        Assert.assertEquals(expectedCurrentDate.minute, request.eiaData.signaturDato.minute)
    }
}
