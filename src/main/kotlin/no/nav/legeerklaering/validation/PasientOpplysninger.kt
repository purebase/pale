package no.nav.legeerklaering.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import java.time.LocalDate

fun validatePersonalInformation(fellesformat: EIFellesformat, person: Person): List<OutcomeType> {
    val outcome = mutableListOf<OutcomeType>()

    val legeerklaering = extractLegeerklaering(fellesformat)

    val patientPersonNumber = extractPersonNumber(legeerklaering)
    val doctorPersonNumber = extractDoctorPersonNumberFromSender(fellesformat)
    val patientBornDate = extractBornDate(patientPersonNumber)

    if (doctorPersonNumber == patientPersonNumber) {
        outcome.add(OutcomeType.BEHANDLER_ER_PASIENT)
    }

    if (person.doedsdato == null) {
        outcome.add(OutcomeType.REGISTRERT_DOD_I_TPS)
    }

    if (LocalDate.now().minusYears(70).isBefore(patientBornDate)) {
        outcome.add(OutcomeType.PASIENT_OVER_70)
    }

    return outcome
}
