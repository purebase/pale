package no.nav.pale.client

import no.nav.pale.validation.extractLegeerklaering
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.PaleConstant
import no.nav.pale.getHCPFodselsnummer
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Test


class ArenaClientTest {

    private val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
    private val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)

    private val tssid = "12454"
    private val navkontor = "0301"
    private val request: ArenaEiaInfo = createArenaEiaInfo(fellesformat, tssid, null, navkontor)

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
}
