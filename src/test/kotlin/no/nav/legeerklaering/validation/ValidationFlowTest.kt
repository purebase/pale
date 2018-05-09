package no.nav.legeerklaering.validation

import junit.framework.Assert.assertEquals
import no.nav.legeerklaering.readToFellesformat
import org.junit.Assert
import org.junit.Test

class ValidationFlowTest{

    @Test
    fun shouldCreatePatientPersonNumberNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientFodselsnummerIkkeFunnet.xml")

        val outcomeList = validationFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND }

        Assert.assertEquals(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, outcome?.outcomeType)

    }

    @Test
    fun shouldCreateOutcomePersonNumberNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringFodselsnummerEllerDnummerIkkeFunnet.xml")
        val name = "Inga Fos Valda"
        val excpectederknadsTekst = "Fødselsnummeret eller D-nummeret til $name finnes ikke i skjemaet."

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.PERSON_NUMBER_NOT_FOUND }

        assertEquals(OutcomeType.PERSON_NUMBER_NOT_FOUND, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }
}
