package no.nav.pale.client

import net.logstash.logback.argument.StructuredArguments
import no.nav.pale.objectMapper
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.util.*

class PdfClient(private val baseUrl: String) {
    private val base64Encoder: Base64.Encoder = Base64.getEncoder()
    private val client: OkHttpClient = OkHttpClient()

    fun generatePDFBase64(pdfType: PdfType, domainObject: Any): ByteArray {
        val request = Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsBytes(domainObject)))
                .url("$baseUrl/${pdfType.pdfGenName()}")
                .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val bytes = response.body()?.bytes()
            if (bytes != null) {
                return base64Encoder.encode(bytes)
            }
            throw IOException("Received no body from the PDF generator")
        } else {
            log.error("Received an error while contacting the PDF generator {}", StructuredArguments.keyValue("errorBody", response.body()?.string()))
            throw IOException("Unable to contact the PDF generator, got status code ${response.code()}")
        }
    }
}

enum class PdfType {
    FAGMELDING,
    BEHANDLINGSVEDLEGG
}

fun PdfType.pdfGenName(): String =
        "legeerklaering_${name.toLowerCase()}"
