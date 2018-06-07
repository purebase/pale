package no.nav.pale.validation

import no.nav.pale.RelationType
import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

fun postTPSFlow(fellesformat: EIFellesformat, personTPS: Person): List<Outcome> =
        initFlow(fellesformat)
                .map {
                    it to personTPS
                }
                .doOnNext {
                    (executionInfo, person) ->
                    when (person.diskresjonskode?.value) {
                        // Sperret adresse, strengt fortrolig
                        "SPSF" -> executionInfo.outcome += OutcomeType.PATIENT_HAS_SPERREKODE_6
                        // Sperret adresse, fortrolig
                        "SPFO" -> executionInfo.outcome += OutcomeType.PATIENT_HAS_SPERREKODE_7
                    }
                }
                .doOnNext {
                    (executionInfo, person) ->
                    if (person.doedsdato != null) {
                        executionInfo.outcome += OutcomeType.REGISTERED_DEAD_IN_TPS
                    }
                }
                .doOnNext {
                    (executionInfo, person) ->
                    val relations = findDoctorInRelations(person, executionInfo.doctorIdent!!.id)

                    if (relations != null) {
                        val outcomeType = when (RelationType.fromKodeverkValue(relations.tilRolle.value)) {
                            RelationType.EKTEFELLE -> OutcomeType.MARRIED_TO_PATIENT
                            RelationType.SAMBOER -> OutcomeType.COHABITANT_WITH_PATIENT
                            RelationType.REGISTRERT_PARTNER_MED -> OutcomeType.REGISTERED_PARTNER_WITH_PATIENT
                            RelationType.FAR -> OutcomeType.PARENT_TO_PATIENT
                            RelationType.MOR -> OutcomeType.PARENT_TO_PATIENT
                            RelationType.BARN -> OutcomeType.CHILD_OF_PATIENT
                            RelationType.GIFT_LEVER_ADSKILT -> OutcomeType.MARIED_LIVES_SEPERATED
                            in RelationType.values() -> null
                            else -> throw RuntimeException("Found relation type \"${relations.tilRolle.value}\" that's not registered in the application")
                        }
                        if (outcomeType != null) {
                            executionInfo.outcome += outcomeType
                        }
                    }
                }
                .doOnNext {
                    (executionInfo, person) ->
                    if (person.personstatus != null && person.personstatus.personstatus != null) {
                        if (person.personstatus.personstatus.value == "UTVA") {
                            executionInfo.outcome += OutcomeType.PATIENT_EMIGRATED

                        }
                    }
                }
                .doOnNext { (executionInfo, _) -> collectFlowStatistics(executionInfo.outcome) }
                .firstElement().blockingGet().first.outcome