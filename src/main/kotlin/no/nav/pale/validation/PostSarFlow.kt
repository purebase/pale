package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.client.Samhandler
import no.nav.pale.findBestSamhandlerPraksis

fun postSARFlow(fellesformat: EIFellesformat, samhandler: List<Samhandler>): List<Outcome> {
    val outcome = mutableListOf<Outcome>()

    val samhandlerPraksis = findBestSamhandlerPraksis(samhandler, fellesformat)?.samhandlerPraksis

    if (samhandlerPraksis == null) {
        outcome += OutcomeType.BEHANDLER_NOT_SAR.toOutcome()
        return outcome
    }


    if (samhandlerPraksis.arbeids_adresse_linje_1 == null || samhandlerPraksis.arbeids_adresse_linje_1.isEmpty()) {
        outcome += OutcomeType.ADDRESS_MISSING_SAR
    }

    if (samhandlerPraksis.samh_praksis_type_kode in arrayOf("LEVA", "LEKO")) {
        outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
    }

    val samhandlerIdentFnr = samhandler.flatMap{ it.samh_ident }
            .filter {
                it.ident_type_kode == "FNR"
                }
            .filter {
                it.aktiv_ident == "1"
            }

    if(samhandlerIdentFnr.isNotEmpty())
    {
        outcome += OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR
    }

    val samhandlerIdentLEKITLMT = samhandler
            .filter {
                it.samh_type_kode == "LE"|| it.samh_type_kode == "KI" ||it.samh_type_kode == "TL" || it.samh_type_kode == "MT"
            }

    if(samhandlerIdentLEKITLMT.isEmpty())
    {
        outcome += OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR
    }

    collectFlowStatistics(outcome)
    return outcome
}
