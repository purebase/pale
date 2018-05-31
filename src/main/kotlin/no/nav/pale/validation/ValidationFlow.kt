package no.nav.pale.validation

import no.nav.pale.mapping.ApprecError
import no.nav.pale.metrics.APPREC_ERROR_COUNTER
import no.nav.pale.validatePersonAndDNumber
import no.nav.pale.validatePersonAndDNumber11Digits
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
                    val patient = it.legeerklaering.pasientopplysninger.pasient.navn
                    val patientName = "${patient.etternavn} ${patient.fornavn} ${patient.mellomnavn}"
                    if (it.patientIdent == null || it.patientIdent.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.toOutcome(
                                apprecError = ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA.v).inc()
                    } else if (!validatePersonAndDNumber11Digits(it.patientIdent)) {
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(patientName, it.patientIdent,
                                it.patientIdent.length, apprecError = ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.v).inc()
                    } else if (!validatePersonAndDNumber(it.patientIdent)) {
                        it.outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(patientName,
                                it.patientIdent, apprecError = ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG)
                        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.v).inc()

                    }
                }
                .doOnNext {
                    val hcp = it.fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional;
                    val name = "${hcp.familyName} ${hcp.givenName} ${hcp.middleName}"
                    if (it.doctorIdent?.id == null || it.doctorIdent.id.trim().isEmpty()) {
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_FOUND.toOutcome(name,
                                apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
                    } else if (!validatePersonAndDNumber11Digits(it.doctorIdent.id)) {
                        it.outcome += OutcomeType.PERSON_NUMBER_NOT_11_DIGITS.toOutcome(name, it.doctorIdent,
                                it.doctorIdent.id.length, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
                    } else if (!validatePersonAndDNumber(it.doctorIdent.id)) {
                        it.outcome += OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER.toOutcome(name,
                                it.doctorIdent, apprecError = ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID)
                    }
                }
                .doOnNext {
                    val surname = extractPatientSurname(it.legeerklaering)

                    if (surname == null || surname.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_SURNAME_NOT_FOUND.toOutcome(
                                apprecError = ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA)
                    }
                }
                .doOnNext {
                    val firstName = extractPatientFirstName(it.legeerklaering)

                    if (firstName == null || firstName.trim().isEmpty()) {
                        it.outcome += OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND.toOutcome(
                                apprecError = ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA)
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
                                it.fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime(),
                                it.fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime(),
                                apprecError = ApprecError.SIGNATURE_ERROR)
                    }
                }
                .doOnNext { collectFlowStatistics(it.outcome) }
                .firstElement().blockingGet().outcome
