package no.nav.legeerklaering

import no.nav.model.fellesformat.EIFellesformat
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.transform.stream.StreamSource

object Utils {
    @Throws(IOException::class, URISyntaxException::class)
    fun readToString(resource: String): String {
        return String(Files.readAllBytes(Paths.get(Utils::class.java.getResource(resource).toURI())), Charset.forName("UTF-8"))
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun readToFellesformat(resource: String): EIFellesformat {
        return fellesformatUnmarshaller.unmarshal(StreamSource(Utils::class.java.getResourceAsStream(resource)), EIFellesformat::class.java).value
    }
}
