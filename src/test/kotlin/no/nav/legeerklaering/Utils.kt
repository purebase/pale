package no.nav.legeerklaering

import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object Utils {
    @Throws(IOException::class, URISyntaxException::class)
    fun readToString(resource: String): String {
        return String(Files.readAllBytes(Paths.get(Utils::class.java.getResource(resource).toURI())), Charset.forName("UTF-8"))
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun readToFellesformat(resource: String): EIFellesformat {
        return objectMapper.readValue<EIFellesformat>(Files.readAllBytes(Paths.get(Utils::class.java.getResource(resource).toURI())), EIFellesformat::class.java)
    }

    //TODO should Remove this
    @Throws(IOException::class, URISyntaxException::class)
    fun readToLegerklearing(resource: String): Legeerklaring {
        return objectMapper.readValue<Legeerklaring>(Files.readAllBytes(Paths.get(Utils::class.java.getResource(resource).toURI())), Legeerklaring::class.java)
    }
}
