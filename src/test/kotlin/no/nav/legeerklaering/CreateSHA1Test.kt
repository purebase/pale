package no.nav.legeerklaering;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.*

class CreateSHA1Test {

  @Test
     fun shouldCreateDuplicateHashValues() {
      val inputMeldingString = Utils.readToString("/legeerklaering.xml")

      val firstMessage = LegeerklaeringApplication().createLegeerklaeringHash(inputMeldingString)
      val secoundMessage = LegeerklaeringApplication().createLegeerklaeringHash(inputMeldingString)

      assertEquals(firstMessage, secoundMessage)
     }

    @Test
    fun shouldCreateUniqueHashValues() {
        val firstinputMeldingString = Utils.readToFellesformat("/legeerklaering.xml")

        val firstMessage = LegeerklaeringApplication().createLegeerklaeringHash(firstinputMeldingString.toString())

        for (i in 1..1000) {
            val secoundinputMeldingString = Utils.readToFellesformat("/legeerklaering.xml")
            secoundinputMeldingString.mottakenhetBlokk.ediLoggId = UUID.randomUUID().toString()
            val secoundMessage = LegeerklaeringApplication().createLegeerklaeringHash(secoundinputMeldingString.toString())

            assertNotEquals(firstMessage, secoundMessage)
        }

    }
}
