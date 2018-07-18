package no.nav.pale.datagen

import no.nav.pale.validation.extractIndividualDigits
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

class TestDataGeneratorSpek : Spek({
    // Had an issue with this causing inconsistent test results for integration tests since we were getting person
    // numbers that were generated for the wrong range
    describe("Generating a person born after 2000") {
        val personNumber = generatePersonNumber(LocalDate.of(2005, 1, 1))
        val individualNumber = extractIndividualDigits(personNumber)
        it("Results with a individ number between 500 and 999") {
            individualNumber shouldBeGreaterOrEqualTo 500
            individualNumber shouldBeLessOrEqualTo 999
        }
    }
})
