package no.nav.pale.validation

import no.nav.pale.client.SamhandlerPraksis
import no.nav.model.fellesformat.EIFellesformat

fun postTSSFlow(fellesformat: EIFellesformat, samhandlerPraksis: SamhandlerPraksis): List<Outcome> =
        initFlow(fellesformat)
                .map {
                    it to samhandlerPraksis
                }
                .doOnNext {
                    (executionInfo, samhandlerPraksis) ->
                    if (samhandlerPraksis.arbeids_adresse_linje_1 == null || samhandlerPraksis.arbeids_adresse_linje_1.isEmpty()) {
                        executionInfo.outcome += OutcomeType.ADDRESS_MISSING_TSS
                    }
                }
                .doOnNext {
                    (executionInfo, samhandlerPraksis) ->
                    if (samhandlerPraksis.samh_praksis_type_kode in arrayOf("LEVA", "LEKO")) {
                        executionInfo.outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
                    }
                }
                .firstElement().blockingGet().first.outcome
