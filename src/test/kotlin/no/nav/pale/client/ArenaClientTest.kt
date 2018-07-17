package no.nav.pale.client

import no.nav.pale.validation.extractLegeerklaering
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.PaleConstant
import no.nav.pale.getHCPFodselsnummer
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert.assertEquals
import org.junit.Test

class ArenaClientTest {
    private val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
    private val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)

    private val tssid = "12454"
    private val navkontor = "0301"
    private val request: ArenaEiaInfo = createArenaInfo(fellesformat, tssid, null, navkontor)

    @Test
    fun shouldSetEdiloggId() {
        assertEquals(fellesformat.mottakenhetBlokk.ediLoggId, request.ediloggId)
    }

    @Test
    fun shouldSethendelseStatus() {
        assertEquals(PaleConstant.tilvurdering.string, request.hendelseStatus)
    }

    @Test
    fun shouldSetVersion() {
        assertEquals("2.0", request.version)
    }

    @Test
    fun shouldSetSkjemaTypen() {
        assertEquals(PaleConstant.LE.string, request.skjemaType)
    }

    @Test
    fun shouldSetMappeTypeUP() {
        val fellesformat: EIFellesformat = readToFellesformat("/client/legeerklaeringTypeLegeerklaringUP.xml")
        val request: ArenaEiaInfo = createArenaInfo(fellesformat, tssid, null, navkontor)
        assertEquals(PaleConstant.mappetypeUP.string, request.mappeType)
    }

    @Test
    fun shouldSetMappeTypeYA() {
        val fellesformat: EIFellesformat = readToFellesformat("/client/legeerklaeringTypeLegeerklaringYA.xml")
        val request = createArenaInfo(fellesformat, tssid, null, navkontor)
        assertEquals(PaleConstant.mappetypeYA.string, request.mappeType)
    }

    @Test
    fun shouldSetMappeTypeRP() {
        val fellesformat: EIFellesformat = readToFellesformat("/client/legeerklaeringTypeLegeerklaringRP.xml")
        val request: ArenaEiaInfo = createArenaInfo(fellesformat, tssid, null, navkontor)
        assertEquals(PaleConstant.mappetypeRP.string, request.mappeType)
    }

    @Test
    fun shouldSetMappeTypeSP() {
        val fellesformat: EIFellesformat = readToFellesformat("/client/legeerklaeringTypeLegeerklaringSP.xml")
        val request: ArenaEiaInfo = createArenaInfo(fellesformat, tssid, null, navkontor)
        assertEquals(PaleConstant.mappetypeSP.string, request.mappeType)
    }

    @Test
    fun shouldSetPasientDataFnr() {
        assertEquals(legeerklaring.pasientopplysninger.pasient.fodselsnummer, request.pasientData.fnr)
    }

    @Test
    fun shouldSetPasientDataSperret() {
        assertEquals(true, request.pasientData.isSperret)
    }

    @Test
    fun shouldSetPasientDataTkNummer() {
        assertEquals(navkontor, request.pasientData.tkNummer)
    }

    @Test
    fun shouldSetLegeDataNavn() {
        assertEquals(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName.toUpperCase() + " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName.toUpperCase() + " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName.toUpperCase(), request.legeData.navn)
    }

    @Test
    fun shouldSetLegeDataFnr() {
        assertEquals(getHCPFodselsnummer(fellesformat), request.legeData.fnr)
    }

    @Test
    fun shouldSetLegeDataTssid() {
        assertEquals(tssid, request.legeData.tssid)
    }
}
