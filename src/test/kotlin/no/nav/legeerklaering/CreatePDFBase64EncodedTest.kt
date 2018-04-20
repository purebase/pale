package no.nav.legeerklaering

import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class CreatePDFBase64EncodedTest{

    @Test
    fun shouldCreatePDFBase64Encoded() {
        val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

       val base64String =  LegeerklaeringApplication().createPDFBase64Encoded(legeerklaring)

        //Files.write(Paths.get("C:/Users/K143566/Downloads/testLE.pdf"), base64String, StandardOpenOption.TRUNCATE_EXISTING)
        //println(base64String)

    }


}
