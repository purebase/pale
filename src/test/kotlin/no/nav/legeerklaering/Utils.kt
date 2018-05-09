package no.nav.legeerklaering

import no.nav.model.fellesformat.EIFellesformat
import java.io.IOException
import java.net.URISyntaxException

import javax.xml.transform.stream.StreamSource

    @Throws(IOException::class, URISyntaxException::class)
    fun readToFellesformat(resource: String): EIFellesformat {
        return fellesformatUnmarshaller.unmarshal(StreamSource(LegeerklaeringApplication::class.java.getResourceAsStream(resource)), EIFellesformat::class.java).value
    }
