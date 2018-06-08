package no.nav.pale.validation

import no.nav.pale.client.SamhandlerPraksis
import no.nav.model.fellesformat.EIFellesformat

fun postTSSFlow(fellesformat: EIFellesformat, samhandlerPraksis: SamhandlerPraksis): List<Outcome> {
    val outcome = mutableListOf<Outcome>()
    if (samhandlerPraksis.arbeids_adresse_linje_1 == null || samhandlerPraksis.arbeids_adresse_linje_1.isEmpty()) {
        outcome += OutcomeType.ADDRESS_MISSING_TSS
    }

    if (samhandlerPraksis.samh_praksis_type_kode in arrayOf("LEVA", "LEKO")) {
        outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
    }

    collectFlowStatistics(outcome)
    return outcome
}
