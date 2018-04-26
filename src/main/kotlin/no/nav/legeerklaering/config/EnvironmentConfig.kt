package no.nav.legeerklaering.config

data class EnvironmentConfig(
        val virksomhetPersonV3EndpointURL: String = System.getenv("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
        val tssWSOrganisasjonV4EndpointURL: String = System.getenv("TSSWSORGANISASJON_V4_ENDPOINTURL")
)
