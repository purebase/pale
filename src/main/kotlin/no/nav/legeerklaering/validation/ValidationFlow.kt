package no.nav.legeerklaering.validation

import no.nav.legeerklaering.mapping.ApprecError
import no.nav.legeerklaering.mapping.ApprecStatus
import no.nav.legeerklaering.mapping.createApprec
import no.nav.legeerklaering.mapping.mapApprecErrorToAppRecCV
import no.nav.legeerklaering.metrics.APPREC_ERROR_COUNTER
import no.nav.legeerklaering.metrics.RULE_COUNTER
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
                            patientIdent = extractPersonIdent(legeerklaering),
                            doctorIdent = extractDoctorIdentFromSender(fellesformat),
                            outcome = mutableListOf()
                    )
                }
                .doOnNext {
                    if (it.patientIdent == null || it.patientIdent.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND
                        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA.v).inc()
                    } else if (!validatePersonAndDNumber11Digits(it.patientIdent)) {
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(
                                extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                                        " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn
                                        +" "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn, it.patientIdent, it.patientIdent.length)
                        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG))
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.v).inc()
                    } else if (!validatePersonAndDNumber(it.patientIdent)) {
                        it.outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(
                                extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                                " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn
                                +" "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn,it.patientIdent)
                        val apprec =  createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.v).inc()

                    }
                }
                .doOnNext {
                    if (it.doctorIdent?.id == null || it.doctorIdent.id.trim().isEmpty()) {
                        val hcp = it.fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional;
                        val name = "${hcp.givenName} ${hcp.middleName} ${hcp.familyName}"
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_FOUND.toOutcome(name)
                        } else if (!validatePersonAndDNumber11Digits(it.doctorIdent.id)) {
                            it.outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                                            " " +
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                            " " +
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName,
                                    it.doctorIdent,  it.doctorIdent.id.length)
                        } else if (!validatePersonAndDNumber(it.doctorIdent.id)) {
                            it.outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                                            " " +
                                            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                            " " +
                                            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName,
                                    it.doctorIdent)
                            val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                            apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
                            APPREC_ERROR_COUNTER.labels(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v).inc()
                        }
                        }
                .doOnNext {
                    val surname = extractPatientSurname(it.legeerklaering)

                    if (surname == null || surname.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_SURNAME_NOT_FOUND
                        val apprec =  createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA.v).inc()
                    }
                }
                .doOnNext {
                    val firstName = extractPatientFirstName(it.legeerklaering)

                    if (firstName == null || firstName.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND
                        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA.v).inc()
                    }
                }
                .doOnNext {
                    val doctorPersonNumberFromLegeerklaering = extractDoctorIdentFromSignature(fellesformat)

                    if (doctorPersonNumberFromLegeerklaering != it.doctorIdent!!.id) {
                        it.outcome += OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA
                    }
                }
                .doOnNext {
                    if (extractSignatureDate(it.fellesformat).isAfter(LocalDateTime.now())) {
                        it.outcome += OutcomeType.SIGNATURE_TOO_NEW.toOutcome(
                                fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime(),
                                fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime())
                    }
                }
                .doOnNext {
                    it.outcome.forEach {
                        RULE_COUNTER.labels(it.outcomeType.name).inc()
                    }
                }
                .firstElement().blockingGet().outcome
