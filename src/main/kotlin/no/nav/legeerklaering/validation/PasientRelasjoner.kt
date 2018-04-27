package no.nav.legeerklaering.validation

import no.nav.legeerklaering.RelationType
import no.nav.model.fellesformat.EIFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

fun validatePatientRelations(fellesformat: EIFellesformat, person: Person): OutcomeType? {
    val doctorPersonnumber = extractDoctorPersonNumberFromSender(fellesformat)
    val relations = findDoctorInRelations(person, doctorPersonnumber) ?: return null

    return when (RelationType.fromKodeverkValue(relations.tilRolle.value)) {
        RelationType.EKTEFELLE -> OutcomeType.GIFT_MED_PASIENT
        RelationType.SAMBOER -> OutcomeType.SAMBOER_MED_PASIENT
        RelationType.REGISTRERT_PARTNER_MED -> OutcomeType.REGISTRERT_PARTNER_MED_PASIENT
        RelationType.FAR -> OutcomeType.FORELDER_TIL_PASIENT
        RelationType.MOR -> OutcomeType.FORELDER_TIL_PASIENT
        RelationType.BARN -> OutcomeType.BARN_AV_PASIENT
        RelationType.GIFT_LEVER_ADSKILT -> OutcomeType.GIFT_LEVER_ADSKILT
        in RelationType.values() -> null
        else -> throw RuntimeException("Found relation type \"${relations.tilRolle.value}\" that's not registered in the application")
    }
}
