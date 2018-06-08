package no.nav.pale.validation

import no.nav.pale.client.SamhandlerPraksis

fun postSARFlow(samhandlerPraksis: SamhandlerPraksis): List<Outcome> {
    val outcome = mutableListOf<Outcome>()
    if (samhandlerPraksis.arbeids_adresse_linje_1 == null || samhandlerPraksis.arbeids_adresse_linje_1.isEmpty()) {
        outcome += OutcomeType.ADDRESS_MISSING_SAR
    }

    if (samhandlerPraksis.samh_praksis_type_kode in arrayOf("LEVA", "LEKO")) {
        outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
    }

    collectFlowStatistics(outcome)
    return outcome
}
