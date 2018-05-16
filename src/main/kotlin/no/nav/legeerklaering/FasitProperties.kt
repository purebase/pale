package no.nav.legeerklaering

data class FasitProperties(
        val mqHostname: String = getEnvVar("MQGATEWAY04_HOSTNAME"),
        val mqPort: Int = getEnvVar("MQGATEWAY04_PORT").toInt(),
        val mqQueueManagerName: String = getEnvVar("MQGATEWAY04_NAME"),
        val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME"),
        val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD"),
        val legeerklaeringQueueName: String = getEnvVar("LEGEERKLAERING_QUEUENAME"),
        val arenaQueueName: String = getEnvVar("ARENA_QUEUENAMEEEE"),
        val virksomhetPersonV3EndpointURL: String = getEnvVar("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
        val tssWSOrganisasjonV4EndpointURL: String = getEnvVar("TSSWSORGANISASJON_V4_ENDPOINTURL"),
        val organisasjonEnhetV2EndpointURL: String = getEnvVar("ORGANISASJONENHET_v2_ENDPOINTURL"),
        val srvLegeerklaeringUsername: String = getEnvVar("SRVLEGEERKLAERING_USERNAME"),
        val srvLegeerklaeringPassword: String = getEnvVar("SRVLEGEERKLAERING_PASSWORD"),
        val kuhrSarApiEndpointURL: String = getEnvVar("KUHR_SAR_API_ENDPOINTURL", "https://kuhr-sar-api"),
        val pdfGeneratorEndpointURL: String = getEnvVar("PDF_GENERATOR_ENDPOINTURL")
)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")
