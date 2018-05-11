package no.nav.legeerklaering.validation

import no.nav.legeerklaering.mapping.ApprecError
import no.nav.legeerklaering.mapping.ApprecStatus
import no.nav.legeerklaering.mapping.createApprec
import no.nav.legeerklaering.mapping.mapApprecErrorToAppRecCV
import no.nav.legeerklaering.validatePersonAndDNumber
import no.nav.legeerklaering.validatePersonAndDNumber11Digits
import no.nav.model.fellesformat.EIFellesformat
import java.time.LocalDateTime

fun validationFlow(fellesformat: EIFellesformat): List<Outcome> =
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
                .doOnNext {
                    if (it.patientPersonNumber == null || it.patientPersonNumber.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND
                        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA)
                    } else if (!validatePersonAndDNumber11Digits(it.patientPersonNumber)) {
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(
                                extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                                        " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn
                                        +" "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn, it.patientPersonNumber, it.patientPersonNumber.length)
                        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG))
                    } else if (!validatePersonAndDNumber(it.patientPersonNumber)) {
                        it.outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(
                                extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                                " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn
                                +" "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn,it.patientPersonNumber)
                        val apprec =  createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG)

                    }
                }
                .doOnNext {
                    if (it.doctorPersonNumber == null || it.doctorPersonNumber.trim().isEmpty()) {
                        val hcp = it.fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional;
                        val name = "${hcp.givenName} ${hcp.middleName} ${hcp.familyName}"
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_FOUND.toOutcome(name)
                        } else if (!validatePersonAndDNumber11Digits(it.doctorPersonNumber)) {
                            it.outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                                            " " +
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                            " " +
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName,
                                    it.doctorPersonNumber,  it.doctorPersonNumber.length)
                        } else if (!validatePersonAndDNumber(it.doctorPersonNumber)) {
                            it.outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                                            " " +
                                            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                            " " +
                                            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName,
                                    it.doctorPersonNumber)
                            val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                            apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
                        }
                        }
                .doOnNext {
                    val surname = extractPatientSurname(it.legeerklaering)

                    if (surname == null || surname.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_SURNAME_NOT_FOUND
                        val apprec =  createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA)
                    }
                }
                .doOnNext {
                    val firstName = extractPatientFirstName(it.legeerklaering)

                    if (firstName == null || firstName.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND
                        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA)
                    }
                }
                .doOnNext {
                    val doctorPersonNumberFromLegeerklaering = extractDoctorPersonNumberFromSignature(fellesformat)

                    if (doctorPersonNumberFromLegeerklaering != it.doctorPersonNumber) {
                        it.outcome += OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA
                    }
                }
                .doOnNext {
                    if (extractSignatureDate(it.fellesformat).isAfter(LocalDateTime.now())) {
                        it.outcome += OutcomeType.SIGNATURE_TOO_NEW
                    }
                }
                .firstElement().blockingGet().outcome
