package no.nav.pale

data class FasitProperties(
        val mqHostname: String = getEnvVar("MQGATEWAY04_HOSTNAME"),
        val mqPort: Int = getEnvVar("MQGATEWAY04_PORT").toInt(),
        val mqQueueManagerName: String = getEnvVar("MQGATEWAY04_NAME"),
        val mqChannelName: String = getEnvVar("PALE_CHANNEL_NAME"),
        val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME", "srvappserver"),
        val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD", ""),
        val inputQueueName: String = getEnvVar("PALE_INPUT_QUEUENAME"),
        val arenaQueueName: String = getEnvVar("ARENA_OUTBOUND_QUEUENAME"),
        val personV3EndpointURL: String = getEnvVar("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
        val organisasjonEnhetV2EndpointURL: String = getEnvVar("VIRKSOMHET_ORGANISASJONENHET_V2_ENDPOINTURL"),
        val srvPaleUsername: String = getEnvVar("SRVPALE_USERNAME"),
        val srvPalePassword: String = getEnvVar("SRVPALE_PASSWORD"),
        val kuhrSarApiURL: String = getEnvVar("KUHR_SAR_API_URL", "http://kuhr-sar-api"),
        val pdfGeneratorURL: String = getEnvVar("PDF_GENERATOR_URL", "http://pdf-gen/api"),
        val paleBackoutQueueName: String = getEnvVar("PALE_BACKOUT_QUEUE_QUEUENAME"),
        val securityTokenServiceUrl: String = getEnvVar("SECURITYTOKENSERVICE_URL"),
        val receiptQueueName: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
        val journalbehandlingEndpointURL: String = getEnvVar("JOURNALBEHANDLING_ENDPOINTURL"),
        val appName: String = getEnvVar("APP_NAME"),
        val appVersion: String = getEnvVar("APP_VERSION")
)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")
