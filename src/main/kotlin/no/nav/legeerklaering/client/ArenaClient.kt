package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.getHCPFodselsnummer
import no.nav.legeerklaering.newInstance
import no.nav.legeerklaering.validation.Outcome
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.legeerklaering.validation.extractLegeerklaering
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import java.util.*

fun createArenaEiaInfo(fellesformat: EIFellesformat, outcomeTypeList: List<Outcome>, tssId: String, sperrekode: Int? = null): ArenaEiaInfo = ArenaEiaInfo().apply {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val hcp = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional
    ediloggId = fellesformat.mottakenhetBlokk.ediLoggId
    hendelseStatus = LegeerklaeringConstant.tilvurdering.string
    version = LegeerklaeringConstant.versjon2_0.string
    skjemaType = LegeerklaeringConstant.LE.string
    mappeType = LegeerklaeringConstant.mappetypeRP.string
    pasientData = ArenaEiaInfo.PasientData().apply {
        fnr = legeerklaering.pasientopplysninger.pasient.fodselsnummer
        isSperret = false
        tkNummer = ""
        if (sperrekode != null && (sperrekode == 6 || sperrekode == 7)){
            spesreg = sperrekode
        }
    }
    legeData = ArenaEiaInfo.LegeData().apply {
        navn = "${hcp.familyName.toUpperCase()} ${hcp.givenName.toUpperCase()} ${hcp.middleName.toUpperCase()}"
        fnr = getHCPFodselsnummer(fellesformat)
        tssid = tssId
    }
    eiaData = ArenaEiaInfo.EiaData().apply {

        if (outcomeTypeList.isNotEmpty()) {
            val systemsvarList = outcomeTypeList
                    .map { mapOutcomeTypeToSystemsvar(it) }
            systemSvar.addAll(systemsvarList)
        }

        signaturDato = newInstance.newXMLGregorianCalendar(GregorianCalendar().apply {
            set(fellesformat.msgHead.msgInfo.genDate.year, fellesformat.msgHead.msgInfo.genDate.month,fellesformat.msgHead.msgInfo.genDate.day)
        })

    }
}

fun mapOutcomeTypeToSystemsvar(outcome: Outcome): ArenaEiaInfo.EiaData.SystemSvar = ArenaEiaInfo.EiaData.SystemSvar().apply {
    meldingsNr = outcome.outcomeType.messageNumber.toBigInteger()
    meldingsTekst = outcome.formattedMessage
    meldingsPrioritet = outcome.outcomeType.messagePriority.priorityNumber.toBigInteger()
    meldingsType = outcome.outcomeType.messageType.type
}
