package no.nav.legeerklaering

import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class CreatePDFBase64EncodedTest{

    @Test
    fun shouldCreatePDFBase64Encoded() {

        val legeerklaring = Utils.readToLegerklearing("/legeerklaeringFagmelding.xml")

       val base64String =  LegeerklaeringApplication().createPDFBase64Encoded(legeerklaring)

        Files.write(Paths.get("C:/Users/K143566/Downloads/testLE.pdf"), base64String.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
        //println(base64String)

    }


}