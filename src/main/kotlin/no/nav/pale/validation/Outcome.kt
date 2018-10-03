package no.nav.pale.validation

import no.nav.model.arenainfo.ArenaEiaInfo
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

fun Outcome.toSystemSvar(): ArenaEiaInfo.EiaData.SystemSvar =
        ArenaEiaInfo.EiaData.SystemSvar().apply {
            meldingsPrioritet = outcomeType.messagePriority.priorityNumber.toBigInteger()
            meldingsNr = outcomeType.messageNumber.toBigInteger()
            meldingsTekst = formattedMessage
        }

enum class OutcomeType(val messageNumber: Int, val messageText: String, val messagePriority: Priority) {
    PATIENT_PERSON_NUMBER_NOT_FOUND(13, "Pasientens fødselsnummer finnes ikke i skjema", Priority.RETUR),
    BEHANDLER_NOT_SAR(18, "Behandler er ikke registrert i SAR.", Priority.MANUAL_PROCESSING),
    // This rule is disabled, due to EPJ systems do not send in a fnr or dnr, it is not required.
    // PERSON_NUMBER_NOT_FOUND(30, "Fødselsnummeret eller D-nummeret til %s" + " finnes ikke i skjemaet.", Priority.RETUR),
    INVALID_PERSON_NUMBER_OR_D_NUMBER(31, "Fødselsnummeret eller D-nummeret %2\$s til %1\$s er feil.", Priority.RETUR),
    PATIENT_SURNAME_NOT_FOUND(33, "Pasientens etternavn finnes ikke i skjema.", Priority.RETUR),
    PATIENT_FIRST_NAME_NOT_FOUND(34, "Pasientens fornavn finnes ikke i skjema.", Priority.RETUR),
    PERSON_NUMBER_NOT_11_DIGITS(47, "%s sitt fødselsnummer eller D-nummer %s er ikke 11 tegn. Det er %s tegn langt.", Priority.RETUR),
    PERSON_HAS_NO_NAV_KONTOR(50, "Personen er ikke registrert med lokal NAV-tilhørighet (TK-nr) i Folkeregisteret.", Priority.MANUAL_PROCESSING),
    PATIENT_NOT_FOUND_TPS(53, "Pasientens fødselsnummer eller D-nummer finnes ikke registrert i Folkeregisteret.", Priority.RETUR),
    PATIENT_EMIGRATED(54, "Person er registrert utvandret i Folkeregisteret.", Priority.MANUAL_PROCESSING),
    BEHANDLER_HAS_FNR_USES_DNR(75, "Behandler har angitt D-nummer, men SAR fant gyldig F-nummer.", Priority.NOTE),
    ADDRESS_MISSING_SAR(76, "Adresse mangler i SAR.", Priority.NOTE),

    // Find best result from KUHR sarz
    NO_VALID_TSSID_PRACTICE_TYPE_SAR(77, "Finner ingen gyldig praksistype i SAR.", Priority.MANUAL_PROCESSING),
    BEHANDLER_TSSID_EMERGENCY_ROOM(78, "Funnet TSS ident er legevakt.", Priority.NOTE),
    UNCERTAIN_RESPONSE_SAR(141, "Usikkert svar fra SAR, lav sannsynlighet for identifikasjon av samhandler. Bør verifiseres.", Priority.NOTE),

    // Arena requires this outcome on successful messages if no other outcomes
    LEGEERKLAERING_MOTTAT(245, "Legeerklæring er mottatt.", Priority.NOTE),
    // This should be completely useless but exists in the old code, the message will be denied earlier if it doesn't
    // contain the signature date, and even if it doesn't we'd throw an exception and we wont let it through
    // SIGNATURE_DATE_MISSING(65, "Signaturdato mangler.", Priority.RETUR),

    MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA(221, "Avvik mellom fødselsnummer fra elektronisk signatur og skjemaet.", Priority.RETUR),
    PATIENT_HAS_SPERREKODE_6(248, "Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.", Priority.MANUAL_PROCESSING),
    PATIENT_HAS_SPERREKODE_7(249, "Pasient er registrert med sperrekode 7, sperret adresse, fortrolig.", Priority.NOTE),
    SIGNATURE_TOO_NEW(251, "Melding mottatt til behandling i dag %s er signert med dato %s, og avvises", Priority.RETUR),

    // Pasientopplysninger
    BEHANDLER_IS_PATIENT(350, "Vurder om legeerklæring kan godtas, behandler er pasient.", Priority.RETUR),
    REGISTERED_DEAD_IN_TPS(351, "Legeerklæring til vurdering, personen er registrert død i Folkeregisteret.", Priority.FOLLOW_UP),
    PATIENT_IS_OVER_70(352, "Legeerklæring til vurdering,  personen er over 70 år.", Priority.RETUR),

    // Pasientrelasjoner
    MARRIED_TO_PATIENT(353, "Vurder om legeerklæring kan godtas, behandler er gift med pasient.", Priority.FOLLOW_UP),
    REGISTERED_PARTNER_WITH_PATIENT(355, "Vurder om legeerklæring kan godtas, behandler er registerert partnes med pasient.", Priority.FOLLOW_UP),
    PARENT_TO_PATIENT(356, "Vurder om legeerklæring kan godtas, behandler er foreldre til pasient.", Priority.FOLLOW_UP),
    CHILD_OF_PATIENT(357, "Vurder om legeerklæring kan godtas, behandler er barn av pasient.", Priority.FOLLOW_UP),
    }

fun OutcomeType.toOutcome(vararg args: Any, apprecError: ApprecError? = null): Outcome =
    Outcome(this, args, apprecError = apprecError)

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
