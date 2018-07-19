package no.nav.pale.utils

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.PaleApplication
import no.nav.pale.fellesformatUnmarshaller
import org.apache.commons.io.IOUtils
import sun.nio.ch.IOUtil
import java.io.IOException
import java.net.ServerSocket
import java.net.URISyntaxException
import javax.xml.transform.stream.StreamSource

@Throws(IOException::class, URISyntaxException::class)
fun readToFellesformat(resource: String): EIFellesformat {
    return fellesformatUnmarshaller.unmarshal(StreamSource(PaleApplication::class.java.getResourceAsStream(resource)), EIFellesformat::class.java).value
}

fun readResourceAsString(resource: String): String =
        IOUtils.toString(PaleApplication::class.java.getResourceAsStream(resource), Charsets.UTF_8)

fun randomPort(): Int = ServerSocket(0).use { it.localPort }
