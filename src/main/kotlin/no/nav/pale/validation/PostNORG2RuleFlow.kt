package no.nav.pale.validation

import no.nav.pale.mapping.ApprecError
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet

fun postNORG2Flow(navKontor: Organisasjonsenhet?): List<Outcome> {
    val outcome = mutableListOf<Outcome>()
    if (navKontor?.enhetId == null || navKontor.enhetId.isEmpty()) {
        outcome += OutcomeType.PERSON_HAS_NO_NAV_KONTOR.toOutcome(apprecError = ApprecError.MISSING_PATIENT_INFO)
    }

    collectFlowStatistics(outcome)
    return outcome
}
