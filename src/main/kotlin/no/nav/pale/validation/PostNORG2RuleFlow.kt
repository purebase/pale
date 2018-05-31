package no.nav.pale.validation

import no.nav.pale.mapping.ApprecError
import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet

fun postNORG2Flow(fellesformat: EIFellesformat, navKontor: Organisasjonsenhet): List<Outcome> = initFlow(fellesformat)
        .doOnNext {
            if (navKontor.enhetId == null || navKontor.enhetId.isEmpty()) {
                it.outcome += OutcomeType.PERSON_HAS_NO_NAV_KONTOR.toOutcome(apprecError = ApprecError.MISSING_PATIENT_INFO)
            }
        }
        .doOnNext { collectFlowStatistics(it.outcome) }
        .firstElement().blockingGet().outcome
