package no.nav.pale.validation

import no.nav.pale.utils.shouldContainOutcome
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet
import org.amshove.kluent.shouldBeEmpty
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostNORG2RuleFlowSpek : Spek({
    describe("No enhetsid in organisasjonsenhet call") {
        val navOffice = Organisasjonsenhet().apply {
            enhetId = ""
            enhetNavn = "NAV Sagene"
        }
        it("Creates outcome for missing NAV office") {
            postNORG2Flow(navOffice) shouldContainOutcome OutcomeType.PERSON_HAS_NO_NAV_KONTOR
        }
    }
    describe("Organisasjonsenhet call with enhetsid") {
        val navOffice = Organisasjonsenhet().apply {
            enhetId = "1234"
            enhetNavn = "NAV Sagene"
        }
        it("Results in no outcome") {
            postNORG2Flow(navOffice).shouldBeEmpty()
        }
    }
})
