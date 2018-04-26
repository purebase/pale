package no.nav.legeerklaering

enum class RelationType(val kodeverkVerdi: String) {
    MOR("MORA"),
    FAR("FARA"),
    BARN("BARN"),
    FOSTERMOR("FOMO"),
    FOSTERFAR("FOFA"),
    FOSTERBARN("FOBA"),
    EKTEFELLE("EKTE"),
    ENKE("ENKE"),
    SKILT("SKIL"),
    SEPARERT("SEPR"),
    REGISTRERT_PARTNER_MED("REPA"),
    SEPARERT_PARTNER("SEPA"),
    SKILT_PARTNER("SKPA"),
    GJENLEVENDE_PARTNER("GJPA"),
    GIFT_LEVER_ADSKILT("GLAD"),
    SAMBOER("SAMB")
    ;

    companion object {
        fun fromKodeverkValue(kodeverkValue: String): RelationType? {
            return RelationType.values().find { it.kodeverkVerdi == kodeverkValue }
        }
    }
}
