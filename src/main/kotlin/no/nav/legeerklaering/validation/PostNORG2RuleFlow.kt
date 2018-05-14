package no.nav.legeerklaering.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.FinnNAVKontorResponse

fun postNORG2Flow(fellesformat: EIFellesformat, finnNAVKontorResponse: FinnNAVKontorResponse): List<Outcome> =
        initFlow(fellesformat)
                .doOnNext {
                    if (finnNAVKontorResponse.navKontor.enhetId != null && finnNAVKontorResponse.navKontor.enhetId.isNotEmpty()) {
                        it.outcome += OutcomeType.PERSON_HAS_NO_NAV_KONTOR
                    }
                }

                .firstElement().blockingGet().outcome
