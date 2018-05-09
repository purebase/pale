package no.nav.legeerklaering.validation

import no.nav.legeerklaering.readToFellesformat
import org.junit.Assert
import org.junit.Test

class ValidationFlowTest{

    @Test
    fun shouldCreatePatientPersonNumberNotFoundMessageText() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientFodselsnummerIkkeFunnet.xml")
        val excpectederknadsTekst = OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.messageText

        val outcomeList = validationFlow(fellesformat)
        val outcome = outcomeList.find { it == OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND }

        Assert.assertEquals(excpectederknadsTekst, outcome?.messageText ?: Assert.fail())

    }

    @Test
    fun shouldCreateMerkandTekst30() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringFodselsnummerEllerDnummerIkkeFunnet.xml")
        val fnr = "12454"
        val excpectederknadsTekst = "Fødselsnummeret eller D-nummeret til "+ fnr +" finnes ikke i skjemaet."

        val outcomeList = validationFlow(fellesformat)

        for( i in outcomeList.indices){
            if (outcomeList[i] == OutcomeType.PERSON_NUMBER_NOT_FOUND) {
                val outcome = Outcome(outcomeList[i], arrayOf(fnr))
                val merknadsTekst = String.format(outcome.outcomeType.messageText, outcome.args[0])
                Assert.assertEquals(excpectederknadsTekst, merknadsTekst)
            }
        }
    }
}