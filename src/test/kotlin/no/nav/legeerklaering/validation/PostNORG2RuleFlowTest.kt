package no.nav.legeerklaering.validation

import no.nav.legeerklaering.readToFellesformat
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet
import org.junit.Assert
import org.junit.Test

class PostNORG2RuleFlowTest {

    val fellesformat = readToFellesformat("/legeerklaering.xml")

    @Test
    fun shouldCreateOutcomeTypePersonHasNoNavKontorWhenEmptyEnghetId() {
        val navKontor = Organisasjonsenhet().apply {
            enhetId = ""
            enhetNavn =  "NAV Sagene"
        }

        val outcomeList = postNORG2Flow(fellesformat, navKontor)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PERSON_HAS_NO_NAV_KONTOR }

        Assert.assertEquals(OutcomeType.PERSON_HAS_NO_NAV_KONTOR, outcome?.outcomeType)
    }

    @Test
    fun shouldNotCreateOutcomeTypePersonHasNoNavKontor() {
        val navKontor = Organisasjonsenhet().apply {
            enhetId = "1234"
            enhetNavn =  "NAV Sagene"
        }

        val outcomeList = postNORG2Flow(fellesformat, navKontor)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PERSON_HAS_NO_NAV_KONTOR }

        Assert.assertEquals(null, outcome?.outcomeType)
    }

}