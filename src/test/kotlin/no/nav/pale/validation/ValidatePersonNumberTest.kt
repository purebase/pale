package no.nav.pale.validation

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ValidatePersonNumberTest : Spek({
    describe("Tests for person number validator") {
        it("Fails when person number is more than 11 characters") {
            validatePersonAndDNumber("300631044141").shouldBeFalse()
        }
        it("Returns true on valid person number") {
            validatePersonAndDNumber("30063104414").shouldBeTrue()
        }
        it("Returns true on valid DNR") {
            validatePersonAndDNumber("45069800525").shouldBeTrue()
        }
        it("Fails when checksum part 1 fails") {
            validatePersonAndDNumber("30063104424").shouldBeFalse()
        }
        it("Fails when checksum part 2 fails") {
            validatePersonAndDNumber("30063104415").shouldBeFalse()
        }
        it("Fails when checksum part 1 is 10") {
            validatePersonAndDNumber("30083104414").shouldBeFalse()
        }
    }
})
