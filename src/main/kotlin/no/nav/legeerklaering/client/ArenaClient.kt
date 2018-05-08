package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringApplication
import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.newInstance
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import java.util.*

class ArenaClient{
    fun createArenaEiaInfo(legeeklaering: Legeerklaring, fellesformat: EIFellesformat, spesregInt: Int, outcomeTypeList: List<OutcomeType>, tssidString: String): ArenaEiaInfo = ArenaEiaInfo().apply {
        ediloggId = fellesformat.mottakenhetBlokk.ediLoggId
        hendelseStatus = LegeerklaeringConstant.tilvurdering.string
        version = LegeerklaeringConstant.versjon2_0.string
        skjemaType = LegeerklaeringConstant.LE.string
        mappeType = LegeerklaeringConstant.mappetypeRP.string
        pasientData = ArenaEiaInfo.PasientData().apply {
            fnr = legeeklaering.pasientopplysninger.pasient.fodselsnummer
            isSperret = false
            tkNummer = ""
            if (spesregInt == 6 || spesregInt == 7){
                spesreg = spesregInt
            }
        }
        legeData = ArenaEiaInfo.LegeData().apply {
            navn = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName
            fnr = LegeerklaeringApplication().getHCPFodselsnummer(fellesformat)
            tssid = tssidString
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

    fun mapOutcomeTypeToSystemsvar(outcomeType :OutcomeType): ArenaEiaInfo.EiaData.SystemSvar = ArenaEiaInfo.EiaData.SystemSvar().apply {
        meldingsNr = outcomeType.messageNumber.toBigInteger()
        meldingsTekst = outcomeType.messageText
        meldingsPrioritet = outcomeType.messagePriority.priorityNumber.toBigInteger()
        meldingsType = outcomeType.messageType.toString()
        }
}