package no.nav.pale.validation

import no.nav.pale.client.SamhandlerIdent
import no.nav.pale.client.SamhandlerPraksis

fun postSARFlow(samhandlerPraksis: SamhandlerPraksis, samhandlerIdentList: List<SamhandlerIdent>): List<Outcome> {
    val outcome = mutableListOf<Outcome>()
    if (samhandlerPraksis.arbeids_adresse_linje_1 == null || samhandlerPraksis.arbeids_adresse_linje_1.isEmpty()) {
        outcome += OutcomeType.ADDRESS_MISSING_SAR
    }

    if (samhandlerPraksis.samh_praksis_type_kode in arrayOf("LEVA", "LEKO")) {
        outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
    }

    val samhandlerIdent = samhandlerIdentList
            .filter {
                it.ident_type_kode == "FNR"
                }
            .filter {
                it.aktiv_ident == "1"
            }

    if(samhandlerIdent.isNotEmpty())
    {
        outcome += OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR
    }

    collectFlowStatistics(outcome)
    return outcome
}
