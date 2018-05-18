package no.nav.legeerklaering.validation

import no.nav.legeerklaering.readToFellesformat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class PreTPSRuleFlowTest {

    @Test
    fun shouldCreateOutcomeTypePasientOver70() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientOver70.xml")

        val outcomeList = preTPSFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PASIENT_OVER_70 }

        assertEquals(OutcomeType.PASIENT_OVER_70, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypePasientOver70Dnummer() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientOver70Dnummer.xml")

        val outcomeList = preTPSFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PASIENT_OVER_70 }

        Assert.assertEquals(OutcomeType.PASIENT_OVER_70, outcome?.outcomeType)

    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerErPasient() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringBehandlerErPasient.xml")

        val outcomeList = preTPSFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_ER_PASIENT }

        Assert.assertEquals(OutcomeType.BEHANDLER_ER_PASIENT, outcome?.outcomeType)

    }

}
