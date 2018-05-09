package no.nav.legeerklaering.validation

import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDate

fun preTSSFlow(fellesformat: EIFellesformat): List<Outcome> = initFlow(fellesformat)
        .doOnNext {
            if (LocalDate.now().minusYears(70).isBefore(extractBornDate(it.patientPersonNumber!!))) {
                it.outcome += OutcomeType.PASIENT_OVER_70
            }
        }
        .doOnNext {
            if (extractDoctorPersonNumberFromSender(it.fellesformat) == extractPersonNumber(it.legeerklaering)) {
                it.outcome += OutcomeType.BEHANDLER_ER_PASIENT
            }
        }
        .firstElement().blockingGet().outcome

