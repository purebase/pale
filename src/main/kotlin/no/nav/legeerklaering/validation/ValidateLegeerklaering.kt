package no.nav.legeerklaering.validation

import no.nav.legeerklaering.validatePersonAndDNumber
import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDateTime

fun doValidatePersonAndDNumber(fellesformat: EIFellesformat): List<OutcomeType> {
    val outcomes = mutableListOf<OutcomeType>()
    val legeerklaering = extractLegeerklaering(fellesformat)
    val patientPersonNumber = extractPersonNumber(legeerklaering)
    val doctorPersonNumber = extractDoctorPersonNumberFromSender(fellesformat)

    if (patientPersonNumber.trim().isEmpty()) {
        outcomes.add(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND)
    }

    if (!validatePersonAndDNumber(patientPersonNumber)) {
        outcomes.add(OutcomeType.INVALID_PERSON_D_NUMBER)
    }

    if (!validatePersonAndDNumber(doctorPersonNumber)) {
        outcomes.add(OutcomeType.INVALID_PERSON_D_NUMBER)
    }

    val surname = extractPatientSurname(legeerklaering)
    if (surname == null || surname.trim().isEmpty()) {
        outcomes.add(OutcomeType.PATIENT_SURNAME_NOT_FOUND)
    }

    val firstName = extractPatientFirstName(legeerklaering)
    if (firstName == null || firstName.trim().isEmpty()) {
        outcomes.add(OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND)
    }

    if (extractSignatureDate(fellesformat).isAfter(LocalDateTime.now())) {
        outcomes.add(OutcomeType.SIGNATURE_TOO_NEW)
    }

    fellesformat.mottakenhetBlokk.mottattDatotid

    return outcomes
}

fun doValidateDoctorPersonNumber(fellesformat: EIFellesformat): List<OutcomeType> {
    val outcomes = mutableListOf<OutcomeType>()
    val legeerklaering = extractLegeerklaering(fellesformat)

    val doctorPersonNumberFromSignature = extractDoctorPersonNumberFromSender(fellesformat)
    val doctorPersonNumberFromLegeerklaering = extractDoctorPersonNumberFromSignature(fellesformat)

    if (doctorPersonNumberFromLegeerklaering != doctorPersonNumberFromSignature) {
        outcomes.add(OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA)
    }

    return outcomes
}
