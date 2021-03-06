package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.mapping.ApprecError
import java.time.LocalDate

fun preTPSFlow(fellesformat: EIFellesformat): List<Outcome> {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val patientIdent = extractPersonIdent(legeerklaering)
    val outcome = mutableListOf<Outcome>()

    if (patientIdent != null && !patientIdent.isEmpty() && patientIdent.length == 11 &&
            extractBornDate(patientIdent).isBefore(LocalDate.now().minusYears(70))) {
        outcome += OutcomeType.PATIENT_IS_OVER_70.toOutcome(
                apprecError = ApprecError.PATIENT_IS_OVER_70)
    }

    if (extractDoctorIdentFromSignature(fellesformat) == extractPersonIdent(legeerklaering)) {
        outcome += OutcomeType.BEHANDLER_IS_PATIENT.toOutcome(
                apprecError = ApprecError.BEHANDLER_IS_PATIENT)
    }
    collectFlowStatistics(outcome)
    return outcome
}
