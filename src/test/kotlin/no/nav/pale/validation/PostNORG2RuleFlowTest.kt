package no.nav.pale.validation

import no.nav.pale.utils.assertOutcomesContain
import no.nav.pale.utils.readToFellesformat
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet
import org.junit.Assert.assertTrue
import org.junit.Test

class PostNORG2RuleFlowTest {

    val fellesformat = readToFellesformat("/legeerklaering.xml")

    @Test
    fun shouldCreateOutcomeTypePersonHasNoNavKontorWhenEmptyEnghetId() {
        val navOffice = Organisasjonsenhet().apply {
            enhetId = ""
            enhetNavn = "NAV Sagene"
        }
        assertOutcomesContain(OutcomeType.PERSON_HAS_NO_NAV_KONTOR, postNORG2Flow(navOffice))
    }

    @Test
    fun shouldNotCreateOutcomeTypePersonHasNoNavKontor() {
        val navOffice = Organisasjonsenhet().apply {
            enhetId = "1234"
            enhetNavn = "NAV Sagene"
        }
        assertTrue("Should not contain any outcomes", postNORG2Flow(navOffice).isEmpty())
    }
}
