package no.nav.legeerklaering

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateApprecTest {

    @Test
    fun shouldCreateOKApprec() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaeringMelding.xml")

        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringMeldingApprec.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat)

        assertEquals(expectedApprecFellesformat.appRec.id, apprecFellesformat.appRec.id)

    }
}