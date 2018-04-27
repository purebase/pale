package no.nav.legeerklaering;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.*

class CreateSHA1Test {

  @Test
     fun shouldCreateDuplicateHashValues() {
      val inputMeldingString = Utils.readToString("/legeerklaering.xml")

      val firstMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes((inputMeldingString)))
      val secoundMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes((inputMeldingString)))

      assertEquals(firstMessage, secoundMessage)
     }

    @Test
    fun shouldCreateUniqueHashValues() {
        val firstinputMeldingString = Utils.readToFellesformat("/legeerklaering.xml")

        val firstMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes(firstinputMeldingString.toString()))

        for (i in 1..1000) {
            val secoundinputMeldingString = Utils.readToFellesformat("/legeerklaering.xml")
            secoundinputMeldingString.mottakenhetBlokk.ediLoggId = UUID.randomUUID().toString()
            val secoundMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes(secoundinputMeldingString.toString()))

            assertNotEquals(firstMessage, secoundMessage)
        }

    }
}
