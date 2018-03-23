package no.nav.legeerklaering

data class FasitProperties(
        val mqHostname: String = getEnvVar("MQGATEWAY04_HOSTNAME"),
        val mqPort: Int = getEnvVar("MQGATEWAY04_PORT").toInt(),
        val mqQueueManagerName: String = getEnvVar("MQGATEWAY04_NAME"),
        val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME"),
        val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD"),
        val legeerklaeringQueueName: String = getEnvVar("LEGEERKLAERING_QUEUENAME"),
        val arenaQueueName: String = getEnvVar("ARENA_QUEUENAMEEEE")
)

fun getEnvVar(name: String): String = System.getenv(name) ?: throw RuntimeException("Missing variable $name")