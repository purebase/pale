package no.nav.pale.client

import no.nav.pale.PaleConstant
import no.nav.pale.getHCPFodselsnummer
import no.nav.pale.validation.extractLegeerklaering
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.AktueltTiltak
import no.nav.pale.mapping.formatName
import java.math.BigInteger

fun createArenaEiaInfo(fellesformat: EIFellesformat, tssId: String?, sperrekode: Int? = null, navkontor: String?): ArenaEiaInfo = ArenaEiaInfo().apply {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val hcp = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional
    ediloggId = fellesformat.mottakenhetBlokk.ediLoggId
    hendelseStatus = PaleConstant.tilvurdering.string
    version = PaleConstant.versjon2_0.string
    skjemaType = PaleConstant.LE.string
    mappeType = findMappeTypeInLegeerklaering(legeerklaering.legeerklaringGjelder.first().typeLegeerklaring)
    pasientData = ArenaEiaInfo.PasientData().apply {
        fnr = legeerklaering.pasientopplysninger.pasient.fodselsnummer
        isSperret = legeerklaering.forbeholdLegeerklaring.tilbakeholdInnhold.toInt() == 2
        tkNummer = navkontor
        if (sperrekode != null && (sperrekode == 6 || sperrekode == 7)) {
            spesreg = sperrekode
        }
    }
    legeData = ArenaEiaInfo.LegeData().apply {
        navn = hcp?.formatName() ?: ""
        fnr = getHCPFodselsnummer(fellesformat)
        tssid = tssId
    }
}

fun findMappeTypeInLegeerklaering(typeLegeerklaring: BigInteger): String =
    when (typeLegeerklaring){
        4.toBigInteger() -> PaleConstant.mappetypeUP.string
        3.toBigInteger() -> PaleConstant.mappetypeYA.string
        2.toBigInteger() -> PaleConstant.mappetypeRP.string
        else -> {PaleConstant.mappetypeSP.string}
    }
