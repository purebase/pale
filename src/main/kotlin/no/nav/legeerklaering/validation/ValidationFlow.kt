package no.nav.legeerklaering.validation

import no.nav.legeerklaering.mapping.ApprecError
import no.nav.legeerklaering.mapping.ApprecMapper
import no.nav.legeerklaering.mapping.ApprecStatus
import no.nav.legeerklaering.validatePersonAndDNumber
import no.nav.legeerklaering.validatePersonAndDNumber11Digits
import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDateTime

fun validationFlow(fellesformat: EIFellesformat): List<OutcomeType> =
        initFlow(fellesformat)
                .map {
                    val legeerklaering = extractLegeerklaering(fellesformat)
                    RuleExecutionInfo(
                            fellesformat = fellesformat,
                            legeerklaering = legeerklaering,
                            patientPersonNumber = extractPersonNumber(legeerklaering),
                            doctorPersonNumber = extractDoctorPersonNumberFromSender(fellesformat),
                            outcome = mutableListOf()
                    )
                }
                .map {
                    if (it.patientPersonNumber == null || it.patientPersonNumber.trim().isEmpty()) {
                        it.outcome.add(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND)
                       val apprec = ApprecMapper().createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(ApprecMapper().mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA))
                    } else if (!validatePersonAndDNumber11Digits(it.patientPersonNumber)) {
                        it.outcome.add(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS)
                        val apprec = ApprecMapper().createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(ApprecMapper().mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG))
                    } else if (!validatePersonAndDNumber(it.patientPersonNumber)) {
                        it.outcome.add(OutcomeType.INVALID_PERSON_D_NUMBER)
                        val apprec =  ApprecMapper().createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(ApprecMapper().mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG))

                    }
                    it
                }
                .map {
                    if (it.doctorPersonNumber == null || it.doctorPersonNumber.trim().isEmpty()) {
                        it.outcome.add(OutcomeType.PERSON_NUMBER_NOT_FOUND)
                    } else if (!validatePersonAndDNumber11Digits(it.doctorPersonNumber)) {
                        it.outcome.add(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS)
                    } else if (!validatePersonAndDNumber(it.doctorPersonNumber)) {
                        it.outcome.add(OutcomeType.INVALID_PERSON_D_NUMBER)
                        val apprec = ApprecMapper().createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(ApprecMapper().mapApprecErrorToAppRecCV(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID))
                    }
                    it
                }
                .map {
                    val surname = extractPatientSurname(it.legeerklaering)

                    if (surname == null || surname.trim().isEmpty()) {
                        it.outcome.add(OutcomeType.PATIENT_SURNAME_NOT_FOUND)
                        val apprec =  ApprecMapper().createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(ApprecMapper().mapApprecErrorToAppRecCV(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA))
                    }
                    it
                }
                .map {
                    val firstName = extractPatientFirstName(it.legeerklaering)

                    if (firstName == null || firstName.trim().isEmpty()) {
                        it.outcome.add(OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND)
                        val apprec = ApprecMapper().createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(ApprecMapper().mapApprecErrorToAppRecCV(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA))
                    }
                    it
                }
                .map {
                    val doctorPersonNumberFromLegeerklaering = extractDoctorPersonNumberFromSignature(fellesformat)

                    if (doctorPersonNumberFromLegeerklaering != it.doctorPersonNumber) {
                        it.outcome.add(OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA)
                    }
                    it
                }
                .map {
                    if (extractSignatureDate(it.fellesformat).isAfter(LocalDateTime.now())) {
                        it.outcome.add(OutcomeType.SIGNATURE_TOO_NEW)
                    }
                    it
                }
                .blockingGet().outcome
