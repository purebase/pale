package no.nav.legeerklaering.validation

enum class OutcomeType(val messageNumber: Int, val messageText: String, val messagePriority: Priority, val messageType: Type) {
    RECEIVED(245, "Legeerklæring er motatatt.", Priority.NOTE, Type.LOG),

    // Pasientopplysninger
    BEHANDLER_ER_PASIENT(350, "Vurder om legeerklæring kan godtas, behandler er pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    REGISTRERT_DOD_I_TPS(351, "Legeerklæring til vurdering, personen er registrert død i Folkeregisteret.", Priority.FOLLOW_UP, Type.FAGLIG),
    PASIENT_OVER_70(352, "Legeerklæring til vurdering,  personen er over 70 år.", Priority.FOLLOW_UP, Type.FAGLIG),

    // Pasientrelasjoner
    GIFT_MED_PASIENT(353, "Vurder om legeerklæring kan godtas, behandler er gift med pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    SAMBOER_MED_PASIENT(354, "Vurder om legeerklæring  kan godtas, behandler er gift med pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    REGISTRERT_PARTNER_MED_PASIENT(355, "Vurder om legeerklæring kan godtas, behandler er registerert partnes med pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    FORELDER_TIL_PASIENT(356, "Vurder om legeerklæring kan godtas, behandler er foreldre til pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    BARN_AV_PASIENT(357, "Vurder om legeerklæring kan godtas, behandler er barn av pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    GIFT_LEVER_ADSKILT(358, "Vurder om legeerklæring kan godtas, behandler er gift med pasienten- lever atskilt.", Priority.FOLLOW_UP, Type.FAGLIG),
}

enum class Priority(val priorityNumber: Int) {
    RETUR(1),
    MANUAL_PROCESSING(2),
    FOLLOW_UP(3),
    NOTE(4)
}

enum class Type(val type: String) {
    FAGLIG("1"),
    TECHNICAL("2"),
    LOG("3")
}
