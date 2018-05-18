package no.nav.legeerklaering;

import no.nav.legeerklaering.validation.extractLegeerklaering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.*

class CreateSHA1Test {

    val fellesformat = readToFellesformat("/legeerklaering.xml")

    @Test
    fun shouldCreateDuplicateHashValues() {

        val firstHash = createHash(objectMapper.writeValueAsBytes((fellesformat)))
        val secondHash = createHash(objectMapper.writeValueAsBytes((fellesformat)))

        assertEquals(firstHash, secondHash)
    }

    @Test
    fun shouldCreateUniqueHashValues() {

        val firstHash = createHash(objectMapper.writeValueAsBytes(fellesformat.toString()))

        for (i in 1..1000) {
            val secondLegeerklaering = extractLegeerklaering(readToFellesformat("/legeerklaering.xml"))
            secondLegeerklaering.andreOpplysninger.opplysning = UUID.randomUUID().toString()
            val secondHash = createLEHash(secondLegeerklaering)

            assertNotEquals(firstHash, secondHash)
        }

    }
}
