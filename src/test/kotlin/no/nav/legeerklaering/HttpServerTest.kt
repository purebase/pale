package no.nav.legeerklaering

import io.ktor.server.engine.ApplicationEngine
import no.nav.legeerklaering.metrics.INPUT_MESSAGE_TIME
import no.nav.legeerklaering.metrics.RULE_COUNTER
import no.nav.legeerklaering.metrics.WS_CALL_TIME
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class HttpServerTest {
    private val client: OkHttpClient = OkHttpClient()

    @Test
    fun testIsReadyJson() {
        validateSelfTestStatusJson("is_ready")
    }

    @Test
    fun testIsReady() {
        validateSelfTest("is_ready")
    }

    @Test
    fun testIsAliveJson() {
        validateSelfTestStatusJson("is_alive")
    }

    @Test
    fun validateIsAlive() {
        validateSelfTest("is_alive")
    }

    fun validateSelfTest(endpoint: String) {
        val request = Request.Builder()
                .url("$baseUrl/$endpoint")
                .get()
                .build()

        val response = client.newCall(request).execute()
        assertTrue(response.isSuccessful)
    }

    fun validateSelfTestStatusJson(endpoint: String) {
        val request = Request.Builder()
                .url("$baseUrl/$endpoint")
                .header("Accept", "application/json")
                .get()
                .build()

        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)
        val status = response.body()?.jsonObject(SelftestStatus::class.java)

        assertNotNull(status?.status)
    }

    @Test
    fun prometheusReturnsResults() {
        INPUT_MESSAGE_TIME.startTimer().use {
            Thread.sleep(20)
        }
        RULE_COUNTER.labels("test_rule").inc()
        WS_CALL_TIME.labels("test_ws").startTimer().use {
            Thread.sleep(12)
        }

        val request = Request.Builder()
                .url("$baseUrl/prometheus")
                .get()
                .build()

        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)
        val metrics = response.body()?.string()

        println(metrics)
        assertNotNull(metrics)
        assertTrue(metrics!!.length > 10)
    }

    companion object {
        private val port = randomPort()
        private val baseUrl = "http://localhost:$port"
        lateinit var applicationEngine: ApplicationEngine

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            applicationEngine = createHttpServer(port)
        }
    }
}

fun <T> ResponseBody.jsonObject(clazz: Class<T>): T =
        objectMapper.readValue(string(), clazz)
