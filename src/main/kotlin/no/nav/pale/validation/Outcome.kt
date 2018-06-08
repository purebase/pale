package no.nav.pale.validation

import no.nav.pale.mapping.ApprecError

data class Outcome(val outcomeType: OutcomeType, val args: Array<out Any>, val apprecError: ApprecError?) {
    init {
        if (outcomeType.messagePriority == Priority.RETUR && apprecError == null) {
            throw RuntimeException("A outcome type without a apprec error should never occur")
        }
    }
    val formattedMessage
            get() = String.format(outcomeType.messageText, *args)
}

enum class OutcomeType(val messageNumber: Int, val messageText: String, val messagePriority: Priority, val messageType: Type) {
    PATIENT_PERSON_NUMBER_NOT_FOUND(13, "Pasientens fødselsnummer finnes ikke i skjema", Priority.RETUR, Type.FAGLIG),
    BEHANDLER_NOT_SAR(18,"Behandler er ikke registrert i SAR.", Priority.MANUAL_PROCESSING, Type.FAGLIG),
    PERSON_NUMBER_NOT_FOUND(30,"Fødselsnummeret eller D-nummeret til %s" + " finnes ikke i skjemaet.", Priority.RETUR, Type.FAGLIG),
    INVALID_PERSON_NUMBER_OR_D_NUMBER(31, "Fødselsnummeret eller D-nummeret %s til %s er feil.", Priority.RETUR, Type.FAGLIG),
    PATIENT_SURNAME_NOT_FOUND(33, "Pasientens etternavn finnes ikke i skjema.", Priority.RETUR, Type.FAGLIG),
    PATIENT_FIRST_NAME_NOT_FOUND(34, "Pasientens fornavn finnes ikke i skjema.", Priority.RETUR, Type.FAGLIG),
    PERSON_NUMBER_NOT_11_DIGITS(47,"%s sitt fødselsnummer eller D-nummer %s er ikke 11 tegn. Det er %s tegn langt.", Priority.RETUR, Type.FAGLIG),
    PERSON_HAS_NO_NAV_KONTOR(50,"Personen er ikke registrert med lokal NAV-tilhørighet (TK-nr) i Folkeregisteret.", Priority.RETUR, Type.FAGLIG),
    PATIENT_NOT_FOUND_TPS(53, "Pasientens fødselsnummer eller D-nummer finnes ikke registrert i Folkeregisteret.", Priority.RETUR, Type.FAGLIG),
    PATIENT_EMIGRATED(54, "Person er registrert utvandret i Folkeregisteret.",  Priority.MANUAL_PROCESSING, Type.FAGLIG),
    BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_TSS(75,"Behandler har angitt D-nummer, men TSS fant gyldig F-nummer.",Priority.NOTE, Type.FAGLIG),
    ADDRESS_MISSING_SAR(76,"Adresse mangler i SAR.", Priority.NOTE, Type.FAGLIG),

    // Find best result from KUHR sar
    NO_VALID_TSSID_PRACTICE_TYPE_SAR(77,"Finner ingen gyldig praksistype i SAR.", Priority.NOTE, Type.FAGLIG),
    BEHANDLER_TSSID_EMERGENCY_ROOM(78,"Funnet TSS ident er legevakt.", Priority.NOTE, Type.FAGLIG),
    UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED(141,"Usikkert svar fra SAR, lav sannsynlighet %s for identifikasjon av samhandler. Bør verifiseres.", Priority.NOTE, Type.TECHNICAL),

    // Arena requires this outcome on successful messages if no other outcomes
    LEGEERKLAERING_MOTTAT(245, "Legeerklæring er mottatt.", Priority.NOTE, Type.FAGLIG),
    // This should be completely useless but exists in the old code, the message will be denied earlier if it doesn't
    // contain the signature date, and even if it doesn't we'd throw an exception and we wont let it through
    //SIGNATURE_DATE_MISSING(65, "Signaturdato mangler.", Priority.RETUR, Type.FAGLIG),
    
    MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA(221, "Avvik mellom fødselsnummer fra elektronisk signatur og skjemaet.", Priority.NOTE, Type.FAGLIG),
    PATIENT_HAS_SPERREKODE_6(248, "Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.", Priority.MANUAL_PROCESSING, Type.FAGLIG),
    PATIENT_HAS_SPERREKODE_7(249, "Pasient er registrert med sperrekode 7, sperret adresse, fortrolig.", Priority.NOTE, Type.FAGLIG),
    SIGNATURE_TOO_NEW(251, "Melding mottatt til behandling i dag %s er signert med dato %s, og avvises", Priority.RETUR, Type.FAGLIG),

    // Pasientopplysninger
    BEHANDLER_IS_PATIENT(350, "Vurder om legeerklæring kan godtas, behandler er pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    REGISTERED_DEAD_IN_TPS(351, "Legeerklæring til vurdering, personen er registrert død i Folkeregisteret.", Priority.FOLLOW_UP, Type.FAGLIG),
    PATIENT_IS_OVER_70(352, "Legeerklæring til vurdering,  personen er over 70 år.", Priority.FOLLOW_UP, Type.FAGLIG),

    // Pasientrelasjoner
    MARRIED_TO_PATIENT(353, "Vurder om legeerklæring kan godtas, behandler er gift med pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    COHABITANT_WITH_PATIENT(354, "Vurder om legeerklæring  kan godtas, behandler er gift med pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    REGISTERED_PARTNER_WITH_PATIENT(355, "Vurder om legeerklæring kan godtas, behandler er registerert partnes med pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    PARENT_TO_PATIENT(356, "Vurder om legeerklæring kan godtas, behandler er foreldre til pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    CHILD_OF_PATIENT(357, "Vurder om legeerklæring kan godtas, behandler er barn av pasient.", Priority.FOLLOW_UP, Type.FAGLIG),
    MARIED_LIVES_SEPERATED(358, "Vurder om legeerklæring kan godtas, behandler er gift med pasienten- lever atskilt.", Priority.FOLLOW_UP, Type.FAGLIG),

    BEHANDLER_PERSON_NUMBER_MISSMATCH_CERTIFICATE(381, "Behandler har endret fødselsnummer, sertifikatet for digital signatur må oppdateres.", Priority.NOTE, Type.FAGLIG),
}

fun OutcomeType.toOutcome(vararg args: Any, apprecError: ApprecError? = null): Outcome
    = Outcome(this, args, apprecError = apprecError)

fun OutcomeType.shouldReturnEarly(): Boolean =
        messagePriority == Priority.RETUR

operator fun MutableList<Outcome>.plusAssign(outcomeType: OutcomeType) {
    this += outcomeType.toOutcome()
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
