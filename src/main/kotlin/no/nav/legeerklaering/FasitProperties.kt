package no.nav.legeerklaering

data class FasitProperties(
        val mqHostname: String = getEnvVar("MQGATEWAY04_HOSTNAME"),
        val mqPort: Int = getEnvVar("MQGATEWAY04_PORT").toInt(),
        val mqQueueManagerName: String = getEnvVar("MQGATEWAY04_NAME"),
        val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME", "srvappserver"),
        val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD", ""),
        val inputQueueName: String = getEnvVar("LEGEERKLAERING_QUEUENAME"),
        val arenaQueueName: String = getEnvVar("ARENA_QUEUENAME"),
        val personV3EndpointURL: String = getEnvVar("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
        val organisasjonEnhetV2EndpointURL: String = getEnvVar("VIRKSOMHET_ORGANISASJONENHET_v2_ENDPOINTURL"),
        val srvLegeerklaeringUsername: String = getEnvVar("SRVLEGEERKLAERING_USERNAME"),
        val srvLegeerklaeringPassword: String = getEnvVar("SRVLEGEERKLAERING_PASSWORD"),
        val kuhrSarApiEndpointURL: String = getEnvVar("KUHR_SAR_API_ENDPOINTURL", "https://kuhr-sar-api"),
        val pdfGeneratorEndpointURL: String = getEnvVar("PDF_GENERATOR_ENDPOINTURL", "https://pdf-gen"),
        val legeerklaeringBackoutQueueName: String = getEnvVar("LEGEERKLAERING_BACKOUT_QUEUENAME"),
        val securityTokenServiceUrl: String = getEnvVar("SECURITYTOKENSERVICE_URL"),
        val receiptQueueName: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
        val journalbehandlingEndpointURL: String = getEnvVar("JOURNALBEHANDLING_ENDPOINTURL"),
        val appName: String = getEnvVar("APP_NAME"),
        val appVersion: String = getEnvVar("APP_VERSION")
)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")
