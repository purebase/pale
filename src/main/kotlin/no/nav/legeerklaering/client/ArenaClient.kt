package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringApplication
import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.newInstance
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import java.util.*

class ArenaClient{
    fun createArenaEiaInfo(legeeklaering: Legeerklaring, fellesformat: EIFellesformat): ArenaEiaInfo = ArenaEiaInfo().apply {
        ediloggId = fellesformat.mottakenhetBlokk.ediLoggId
        hendelseStatus = "TIL_VURDERING" //TODO
        version = "2.0"
        skjemaType = LegeerklaeringConstant.LE.string
        mappeType = "UP"
        pasientData = ArenaEiaInfo.PasientData().apply {
            fnr = legeeklaering.pasientopplysninger.pasient.fodselsnummer
            isSperret = false //TODO
            tkNummer = "" //TODO
        }
        legeData = ArenaEiaInfo.LegeData().apply {
            navn = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName
            fnr = LegeerklaeringApplication().getHCPFodselsnummer(fellesformat)
            tssid = "asdad" //TODO
        }
        eiaData = ArenaEiaInfo.EiaData().apply {
            systemSvar.add(ArenaEiaInfo.EiaData.SystemSvar().apply {
                meldingsNr = 141.toBigInteger() //TODO
                meldingsTekst = "Usikkert svar fra TSS,  lav sannsynlighet (55,8%) for identifikasjon av  samhandler.  Bør verifiseres." //TODO
                meldingsPrioritet = 4.toBigInteger() //TODO
                meldingsType = "2" //TODO
            })
            signaturDato = newInstance.newXMLGregorianCalendar(GregorianCalendar().apply {
                set(fellesformat.msgHead.msgInfo.genDate.year, fellesformat.msgHead.msgInfo.genDate.month,fellesformat.msgHead.msgInfo.genDate.day)
            })

        }
    }
}