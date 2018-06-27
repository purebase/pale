package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDate

fun preTPSFlow(fellesformat: EIFellesformat): List<Outcome> {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val patientIdent = extractPersonIdent(legeerklaering)
    val outcome = mutableListOf<Outcome>()

    if (!patientIdent.isNullOrEmpty() && LocalDate.now().minusYears(70).isBefore(extractBornDate(patientIdent))) {
        outcome += OutcomeType.PATIENT_IS_OVER_70
    }

    if (extractDoctorIdentFromSignature(fellesformat) == extractPersonIdent(legeerklaering)) {
        outcome += OutcomeType.BEHANDLER_IS_PATIENT
    }
    collectFlowStatistics(outcome)
    return outcome
}
