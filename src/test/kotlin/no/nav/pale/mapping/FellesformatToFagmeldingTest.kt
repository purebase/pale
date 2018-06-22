package no.nav.pale.mapping

import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Test

class FellesformatToFagmeldingTest {

    val inputMeldingFellesformat = readToFellesformat("/mapping/legeerklaeringMinimumFields.xml")

    @Test
    fun shouldCreateFagmelding() {

        try {
            mapFellesformatToFagmelding(inputMeldingFellesformat)
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            Assert.fail()
        }
    }
}