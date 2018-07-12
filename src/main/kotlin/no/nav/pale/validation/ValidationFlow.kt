package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.mapping.ApprecError
import no.nav.pale.mapping.formatName

fun validationFlow(fellesformat: EIFellesformat): List<Outcome> {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val patientIdent = extractPersonIdent(legeerklaering)
    val doctorIdentFromSender = extractDoctorIdentFromSender(fellesformat)
    val outcome = mutableListOf<Outcome>()

    val patient = legeerklaering.pasientopplysninger.pasient.navn
    val patientName = if (patient.mellomnavn == null) {
        "${patient.etternavn} ${patient.fornavn}"
    } else {
        "${patient.etternavn} ${patient.fornavn} ${patient.mellomnavn}"
    }
    if (patientIdent == null || patientIdent.trim().isEmpty()) {
        outcome += OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.toOutcome(
                apprecError = ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA)
    } else if (!validatePersonAndDNumber11Digits(patientIdent)) {
        outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(patientName, patientIdent,
                patientIdent.length, apprecError = ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG)
    } else if (!validatePersonAndDNumber(patientIdent)) {
        outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(patientName,
                patientIdent, apprecError = ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG)
    }

    val hcp = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional
    val name = hcp?.formatName() ?: ""
    val doctorPersonNumberFromSignature = extractDoctorIdentFromSignature(fellesformat)
    if (!validatePersonAndDNumber11Digits(doctorPersonNumberFromSignature)) {
        outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(name, doctorPersonNumberFromSignature,
                doctorPersonNumberFromSignature.length, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
    } else if (!validatePersonAndDNumber(doctorPersonNumberFromSignature)) {
        outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(name,
                doctorPersonNumberFromSignature, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
    }

    if (doctorIdentFromSender?.id != null && doctorIdentFromSender.id.trim().isNotEmpty()) {
        if (doctorPersonNumberFromSignature != doctorIdentFromSender.id) {
            outcome += OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA
        } else if (!validatePersonAndDNumber11Digits(doctorIdentFromSender.id)) {
            outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(name, doctorPersonNumberFromSignature,
                    doctorPersonNumberFromSignature.length, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
        } else if (!validatePersonAndDNumber(doctorIdentFromSender.id))
            outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(name,
                    doctorPersonNumberFromSignature,
                    apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
    }

    val surname = extractPatientSurname(legeerklaering)
    if (surname == null || surname.trim().isEmpty()) {
        outcome += OutcomeType.PATIENT_SURNAME_NOT_FOUND.toOutcome(
                apprecError = ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA)
    }

    val firstName = extractPatientFirstName(legeerklaering)
    if (firstName == null || firstName.trim().isEmpty()) {
        outcome += OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND.toOutcome(
                apprecError = ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA)
    }

    val signatureDate = extractSignatureDate(fellesformat)
    val receivedDate = extractReceivedDate(fellesformat)
    if (signatureDate.isAfter(receivedDate)) {
        outcome += OutcomeType.SIGNATURE_TOO_NEW.toOutcome(format(receivedDate), format(signatureDate),
                apprecError = ApprecError.GEN_DATE_ERROR)
    }

    collectFlowStatistics(outcome)
    return outcome
}
