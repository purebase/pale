package no.nav.pale

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
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

fun <T> ResponseBody.jsonObject(clazz: Class<T>): T =
        objectMapper.readValue(string(), clazz)

class HttpServerSpek : Spek({
    val client = OkHttpClient()

    val port = randomPort()
    val baseUrl = "http://localhost:$port"
    val applicationEngine = createHttpServer(port, "TEST")

    describe("Selftest/metrics HTTP server test") {
        fun validateSelfTest(endpoint: String) {
            val request = Request.Builder()
                    .url("$baseUrl/$endpoint")
                    .header("Accept", "text/plain")
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

        it("is_ready should return valid json when using Accept: application/json") {
            validateSelfTestStatusJson("is_ready")
        }
        it("is_alive should return valid json when using Accept: application/json") {
            validateSelfTestStatusJson("is_alive")
        }
        it("is_ready should return ok http code") {
            validateSelfTest("is_ready")
        }
        it("is_alive should return ok http code") {
            validateSelfTest("is_alive")
        }
    }

    describe("Make sure metrics is returned") {
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

        it("Should return metrics on HTTP GET") {
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
    }

    afterGroup {
        applicationEngine.stop(1, 10, TimeUnit.SECONDS)
    }
})
