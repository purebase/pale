package no.nav.pale.utils

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.PaleApplication
import no.nav.pale.fellesformatUnmarshaller
import java.io.IOException
import java.net.ServerSocket
import java.net.URISyntaxException
import javax.xml.transform.stream.StreamSource

@Throws(IOException::class, URISyntaxException::class)
fun readToFellesformat(resource: String): EIFellesformat {
    return fellesformatUnmarshaller.unmarshal(StreamSource(PaleApplication::class.java.getResourceAsStream(resource)), EIFellesformat::class.java).value
}

fun randomPort(): Int = ServerSocket(0).use { it.localPort }
