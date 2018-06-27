package no.nav.pale

import io.ktor.server.engine.ApplicationEngine
import no.nav.pale.metrics.APPREC_ERROR_COUNTER
import no.nav.pale.metrics.APPREC_STATUS_COUNTER
import no.nav.pale.metrics.INCOMING_MESSAGE_COUNTER
import no.nav.pale.metrics.MESSAGE_OUTCOME_COUNTER
import no.nav.pale.metrics.REQUEST_TIME
import no.nav.pale.metrics.RULE_COUNTER
import no.nav.pale.metrics.WS_CALL_TIME
import no.nav.pale.utils.randomPort
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        REQUEST_TIME.startTimer().use {
            Thread.sleep(20)
        }
        INCOMING_MESSAGE_COUNTER.inc()
        RULE_COUNTER.labels("test_rule").inc()
        WS_CALL_TIME.labels("test_ws").startTimer().use {
            Thread.sleep(12)
        }
        APPREC_STATUS_COUNTER.labels("test_ok").inc()
        APPREC_ERROR_COUNTER.labels("test_big_error_happend").inc()
        MESSAGE_OUTCOME_COUNTER.labels(PaleConstant.eiaMan.string).inc()

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
            applicationEngine = createHttpServer(port, "TEST")
        }
    }
}

fun <T> ResponseBody.jsonObject(clazz: Class<T>): T =
        objectMapper.readValue(string(), clazz)
