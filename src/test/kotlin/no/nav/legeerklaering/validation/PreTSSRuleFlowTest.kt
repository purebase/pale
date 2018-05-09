package no.nav.legeerklaering.validation

import no.nav.legeerklaering.readToFellesformat
import org.junit.Assert
import org.junit.Test

class PreTSSRuleFlowTest {

    @Test
    fun shouldCreateOutcomeTypePasientOver70() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientOver70.xml")

        val outcomeList = preTSSFlow(fellesformat)
        val outcome = outcomeList.find { it == OutcomeType.PASIENT_OVER_70 }

        Assert.assertEquals(OutcomeType.PASIENT_OVER_70, outcome)

    }

    @Test
    fun shouldCreateOutcomeTypePasientOver70Dnummer() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientOver70Dnummer.xml")

        val outcomeList = preTSSFlow(fellesformat)
        val outcome = outcomeList.find { it == OutcomeType.PASIENT_OVER_70 }

        Assert.assertEquals(OutcomeType.PASIENT_OVER_70, outcome)

    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerErPasient() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringBehandlerErPasient.xml")

        val outcomeList = preTSSFlow(fellesformat)
        val outcome = outcomeList.find { it == OutcomeType.BEHANDLER_ER_PASIENT }

        Assert.assertEquals(OutcomeType.BEHANDLER_ER_PASIENT, outcome)

    }

}