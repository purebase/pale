package no.nav.pale

import no.nav.model.fellesformat.EIFellesformat
import java.io.IOException
import java.io.StringReader
import java.net.ServerSocket
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths


@Throws(IOException::class, URISyntaxException::class)
fun readToFellesformat(resource: String): EIFellesformat {
   return fellesformatJaxBContext.createUnmarshaller().unmarshal(StringReader(resource.getResource())) as EIFellesformat
}

fun randomPort(): Int = ServerSocket(0).use { it.localPort }

fun String.getResource(): String = String(Files.readAllBytes(Paths.get(PaleApplication::class.java.getResource(this).toURI())),
        Charset.forName("ISO-8859-1"))