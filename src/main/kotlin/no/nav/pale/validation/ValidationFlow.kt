package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.mapping.ApprecError
import no.nav.pale.validatePersonAndDNumber
import no.nav.pale.validatePersonAndDNumber11Digits
import java.time.LocalDateTime

fun validationFlow(fellesformat: EIFellesformat): List<Outcome> {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val patientIdent = extractPersonIdent(legeerklaering)
    val doctorIdent = extractDoctorIdentFromSender(fellesformat)
    val outcome = mutableListOf<Outcome>()

    val patient = legeerklaering.pasientopplysninger.pasient.navn
    val patientName = "${patient.etternavn} ${patient.fornavn} ${patient.mellomnavn}"
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
    val name = "${hcp.familyName} ${hcp.givenName} ${hcp.middleName}"
    if (doctorIdent?.id == null || doctorIdent.id.trim().isEmpty()) {
        outcome += OutcomeType.PERSON_NUMBER_NOT_FOUND.toOutcome(name,
                apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
    } else if (!validatePersonAndDNumber11Digits(doctorIdent.id)) {
        outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(name, doctorIdent,
                doctorIdent.id.length, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
    } else if (!validatePersonAndDNumber(doctorIdent.id)) {
        outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(name,
                doctorIdent, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
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

    val doctorPersonNumberFromLegeerklaering = extractDoctorIdentFromSignature(fellesformat)
    if (doctorPersonNumberFromLegeerklaering != doctorIdent!!.id) {
        outcome += OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA
    }

    if (extractSignatureDate(fellesformat).isAfter(LocalDateTime.now())) {
        outcome += OutcomeType.SIGNATURE_TOO_NEW.toOutcome(
                fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime(),
                fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime(),
                apprecError = ApprecError.SIGNATURE_ERROR)
    }

    collectFlowStatistics(outcome)
    return outcome
}
