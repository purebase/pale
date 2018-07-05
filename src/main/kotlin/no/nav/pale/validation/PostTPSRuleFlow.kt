package no.nav.pale.validation

import no.nav.pale.RelationType
import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

fun postTPSFlow(fellesformat: EIFellesformat, person: Person): List<Outcome> {
    val outcome = mutableListOf<Outcome>()
    val doctorIdent = extractDoctorIdentFromSignature(fellesformat)

    when (person.diskresjonskode?.value) {
    // Sperret adresse, strengt fortrolig
        "SPSF" -> outcome += OutcomeType.PATIENT_HAS_SPERREKODE_6
    // Sperret adresse, fortrolig
        "SPFO" -> outcome += OutcomeType.PATIENT_HAS_SPERREKODE_7
    }

    if (person.doedsdato != null) {
        outcome += OutcomeType.REGISTERED_DEAD_IN_TPS
    }

    val relations = findDoctorInRelations(person, doctorIdent)

    if (relations != null) {
        val outcomeType = when (RelationType.fromKodeverkValue(relations.tilRolle.value)) {
            RelationType.EKTEFELLE -> OutcomeType.MARRIED_TO_PATIENT
            RelationType.REGISTRERT_PARTNER_MED -> OutcomeType.REGISTERED_PARTNER_WITH_PATIENT
            RelationType.FAR -> OutcomeType.PARENT_TO_PATIENT
            RelationType.MOR -> OutcomeType.PARENT_TO_PATIENT
            RelationType.BARN -> OutcomeType.CHILD_OF_PATIENT
            in RelationType.values() -> null
            else -> throw RuntimeException("Found relation type \"${relations.tilRolle.value}\" that's not registered in the application")
        }
        if (outcomeType != null) {
            outcome += outcomeType
        }
    }

    if (person.personstatus != null && person.personstatus.personstatus != null) {
        if (person.personstatus.personstatus.value == "UTVA") {
            outcome += OutcomeType.PATIENT_EMIGRATED
        }
    }

    collectFlowStatistics(outcome)
    return outcome
}
