package no.nav.pale.datagen

import no.nav.pale.validation.extractIndividualDigits
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TestDataGeneratorTest {

    // Had an issue with this causing inconsistent test results for integration tests since we were getting person
    // numbers that were generated for the wrong range
    @Test
    fun testGeneratesValidPersonNumberForPeopleBornAfter2000() {
        val personNumber = generatePersonNumber(LocalDate.of(2005, 1, 1))
        val individualNumber = extractIndividualDigits(personNumber)
        assertTrue("$individualNumber>=500 is not valid for a person number after 2000", individualNumber >= 500)
        assertTrue("$individualNumber<=999 is not valid for a person number after 2000", individualNumber <= 999)
    }
}
