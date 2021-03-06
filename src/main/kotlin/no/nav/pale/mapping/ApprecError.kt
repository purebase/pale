package no.nav.pale.mapping

enum class ApprecError(val v: String, val dn: String, val s: String) {
    BEHANDLER_IS_PATIENT("X99", "Legeerklæring kan ikkje godtas, behandler er pasient.", "2.16.578.1.12.4.1.1.8221"),
    MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA("X99", "Avvik mellom fødselsnummer fra elektronisk signatur og skjemaet.", "2.16.578.1.12.4.1.1.8221"),
    GEN_DATE_ERROR("X99", "Meldingens dato datert(gendate) er nyere enn dagens dato", "2.16.578.1.12.4.1.1.8221"),
    BEHANDLER_PERSON_NUMBER_NOT_VALID("21", "Behandlers fødselsnummer er ikke et gyldig fødselsnummer.",
            "2.16.578.1.12.4.1.1.8222"),
    PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA("30", "Pasientens fødselsnummer finnes ikke i skjemaet.",
            "2.16.578.1.12.4.1.1.8222"),
    PATIENT_PERSON_NUMBER_IS_WRONG("31", "Pasientens fødselsnummer er feil.", "2.16.578.1.12.4.1.1.8222"),
    PATIENT_NAME_IS_NOT_IN_SCHEMA("32", "Pasientens navn finnes ikke på skjemaet.",
            "2.16.578.1.12.4.1.1.8222"),
    PATIENT_LASTNAME_IS_NOT_IN_SCHEMA("33", "Pasientens etternavn finnes ikke på skjemaet.",
            "2.16.578.1.12.4.1.1.8222"),
    PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER("53",
            "Pasientens fødselsnummer eller D-nummer finnes ikke registrert i Folkeregisteret.",
            "2.16.578.1.12.4.1.1.8222"),
    DUPLICATE("54", "Duplikat! - Denne legeerklæringen meldingen er mottatt tidligere. Skal ikke sendes på nytt.",
            "2.16.578.1.12.4.1.1.8222"),
    MISSING_PATIENT_INFO("55", "Pasientopplysninger er utilstrekkelige", "2.16.578.1.12.4.1.1.8222"),
    PATIENT_IS_OVER_70("X99", "Legeerklæring kan ikkje godtas, pasient er over 70 år.", "2.16.578.1.12.4.1.1.8221"),

    // All available apprecs for legeerklæring(OID=8222): https://volven.no/produkt.asp?id=335023&catID=3&subID=8
    // ALL available apprecs general OID=8221: https://volven.no/produkt.asp?id=340320&catID=3&subID=8
}
