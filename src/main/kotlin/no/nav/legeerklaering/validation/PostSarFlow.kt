package no.nav.legeerklaering.validation

import no.nav.legeerklaering.client.SamhandlerPraksis
import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

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
                    if (samhandlerPraksis.samh_praksis_type_kode != null && samhandlerPraksis.samh_praksis_type_kode == TODO()) {
                        executionInfo.outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
                    }
                }
                .firstElement().blockingGet().first.outcome
