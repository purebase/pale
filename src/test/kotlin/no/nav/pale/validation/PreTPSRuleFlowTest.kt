package no.nav.pale.validation

import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class PreTPSRuleFlowTest {

    @Test
    fun shouldCreateOutcomeTypePasientOver70() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientOver70.xml")

        val outcomeList = preTPSFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_IS_OVER_70 }

        assertEquals(OutcomeType.PATIENT_IS_OVER_70, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypePasientOver70Dnummer() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientOver70Dnummer.xml")

        val outcomeList = preTPSFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_IS_OVER_70 }

        Assert.assertEquals(OutcomeType.PATIENT_IS_OVER_70, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerErPasient() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringBehandlerErPasient.xml")

        val outcomeList = preTPSFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_IS_PATIENT }

        Assert.assertEquals(OutcomeType.BEHANDLER_IS_PATIENT, outcome?.outcomeType)
    }
}
