package no.nav.pale.validation

import no.nav.pale.utils.assertOutcomesContain
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostNORG2RuleFlowSpek : Spek({
    describe("No enhetsid in organisasjonsenhet call") {
        val navOffice = Organisasjonsenhet().apply {
            enhetId = ""
            enhetNavn = "NAV Sagene"
        }
        it("Should create outcome for missing NAV office") {
            assertOutcomesContain(OutcomeType.PERSON_HAS_NO_NAV_KONTOR, postNORG2Flow(navOffice))
        }
    }
    describe("Organisasjonsenhet call with enhetsid") {
        val navOffice = Organisasjonsenhet().apply {
            enhetId = "1234"
            enhetNavn = "NAV Sagene"
        }
        it("Should result in no outcome") {
            assertTrue("Should not contain any outcomes", postNORG2Flow(navOffice).isEmpty())
        }
    }
})
