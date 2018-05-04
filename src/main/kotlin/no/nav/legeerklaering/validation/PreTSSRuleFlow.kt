package no.nav.legeerklaering.validation

import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDate

fun preTSSFlow(fellesformat: EIFellesformat): List<OutcomeType> =
        initFlow(fellesformat)
                .map {
                    if (LocalDate.now().minusYears(70).isBefore(extractBornDate(it.patientPersonNumber!!))) {
                        it.outcome.add(OutcomeType.PASIENT_OVER_70)
                    }
                    it
                }
                .map {
                    if (extractDoctorPersonNumberFromSender(it.fellesformat) == extractPersonNumber(it.legeerklaering)) {
                        it.outcome.add(OutcomeType.BEHANDLER_ER_PASIENT)
                    }
                    it
                }
                .blockingGet().outcome
