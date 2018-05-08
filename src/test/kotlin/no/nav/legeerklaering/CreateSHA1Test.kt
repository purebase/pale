package no.nav.legeerklaering;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.*

class CreateSHA1Test {

    val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")

  @Test
     fun shouldCreateDuplicateHashValues() {


      val firstMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes((fellesformat)))
      val secoundMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes((fellesformat)))

      assertEquals(firstMessage, secoundMessage)
     }

    @Test
    fun shouldCreateUniqueHashValues() {

        val firstMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes(fellesformat.toString()))

        for (i in 1..1000) {
            val secoundFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
            secoundFellesformat.mottakenhetBlokk.ediLoggId = UUID.randomUUID().toString()
            val secoundMessage = LegeerklaeringApplication().createSha256Hash(objectMapper.writeValueAsBytes(secoundFellesformat.toString()))

            assertNotEquals(firstMessage, secoundMessage)
        }

    }
}
