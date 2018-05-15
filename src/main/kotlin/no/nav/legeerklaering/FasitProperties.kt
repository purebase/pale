package no.nav.legeerklaering

data class FasitProperties(
        val mqHostname: String = getEnvVar("MQGATEWAY04_HOSTNAME"),
        val mqPort: Int = getEnvVar("MQGATEWAY04_PORT").toInt(),
        val mqQueueManagerName: String = getEnvVar("MQGATEWAY04_NAME"),
        val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME"),
        val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD"),
        val legeerklaeringQueueName: String = getEnvVar("LEGEERKLAERING_QUEUENAME"),
        val arenaQueueName: String = getEnvVar("ARENA_QUEUENAMEEEE"),
        val virksomhetPersonV3EndpointURL: String = System.getenv("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
        val tssWSOrganisasjonV4EndpointURL: String = System.getenv("TSSWSORGANISASJON_V4_ENDPOINTURL"),
        val organisasjonEnhetV2EndpointURL: String = System.getenv("ORGANISASJONENHET_v2_ENDPOINTURL")
)

fun getEnvVar(name: String): String = System.getenv(name) ?: throw RuntimeException("Missing variable $name")
