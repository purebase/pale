package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.fellesformatJaxBContext
import no.nav.pale.getResource
import no.nav.pale.readToFellesformat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader

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
