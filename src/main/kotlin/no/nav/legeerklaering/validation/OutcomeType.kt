package no.nav.legeerklaering.validation

enum class OutcomeType(val messageNumber: Int, val messageText: String, val messagePriority: Priority, val messageType: Type) {
    PATIENT_PERSON_NUMBER_NOT_FOUND(13, "Pasientens fødselsnummer finnes ikke i skjema", Priority.RETUR, Type.FAGLIG),
    PERSON_NUMBER_NOT_FOUND(30,"Fødselsnummeret eller D-nummeret til \"%s\" finnes ikke i skjemaet.", Priority.RETUR, Type.FAGLIG),
    INVALID_PERSON_D_NUMBER(31, "Fødselsnummeret eller D-nummeret \"%s\" til %s er feil.", Priority.RETUR, Type.FAGLIG),
    PATIENT_SURNAME_NOT_FOUND(33, "Pasientens etternavn finnes ikke i skjema.", Priority.RETUR, Type.FAGLIG),
    PATIENT_FIRST_NAME_NOT_FOUND(34, "Pasientens fornavn finnes ikke i skjema.", Priority.RETUR, Type.FAGLIG),
    PERSON_NUMBER_NOT_11_DIGITS(47,"\"%s\" sitt fødselsnummer eller D-nummer \"%s\" er ikke 11 tegn. Det er \"%s\" tegn langt.", Priority.RETUR, Type.FAGLIG),
    PATIENT_NOT_FOUND_IN_TPS(53, "Pasientens fødselsnummer eller D-nummer finnes ikke registrert i Folkeregisteret.", Priority.RETUR, Type.FAGLIG),

    // This should be completely useless but exists in the old code, the message will be denied earlier if it doesn't
    // contain the signature date, and even if it doesn't we'd throw an exception and we wont let it through
    //SIGNATURE_DATE_MISSING(65, "Signaturdato mangler.", Priority.RETUR, Type.FAGLIG),
    MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA(221, "Avvik mellom fødselsnummer fra elektronisk signatur og skjemaet.", Priority.NOTE, Type.FAGLIG),
    RECEIVED(245, "Legeerklæring er motatatt.", Priority.NOTE, Type.LOG),
    PATIENT_HAS_SPERREKODE_6(248, "Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.", Priority.MANUAL_PROCESSING, Type.FAGLIG),
    PATIENT_HAS_SPERREKODE_7(249, "Pasient er registrert med sperrekode 7, sperret adresse, fortrolig.", Priority.NOTE, Type.FAGLIG),
    SIGNATURE_TOO_NEW(251, "Melding mottatt til behandling i dag %s er signert med dato %s, og avvises", Priority.RETUR, Type.FAGLIG),

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

    DUPLICATE(820, "Sykemeldingen er mottatt før, dette er et duplikat.", Priority.NOTE, Type.FAGLIG)
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