package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDate

fun preTPSFlow(fellesformat: EIFellesformat): List<Outcome> = initFlow(fellesformat)
        .doOnNext {
            if (LocalDate.now().minusYears(70).isBefore(extractBornDate(it.patientIdent!!))) {
                it.outcome += OutcomeType.PATIENT_IS_OVER_70
            }
        }
        .doOnNext {
            if (extractDoctorIdentFromSender(it.fellesformat)?.id == extractPersonIdent(it.legeerklaering)) {
                it.outcome += OutcomeType.BEHANDLER_IS_PATIENT
            }
        }
        .doOnNext { collectFlowStatistics(it.outcome) }
        .firstElement().blockingGet().outcome

