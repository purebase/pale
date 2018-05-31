package no.nav.pale.validation


import no.nav.pale.validatePersonAndDNumber
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatePersonNumberTest {

    @Test
    fun shouldFailWhePersonNumberIsMoreThan11characters() {
        assertFalse(validatePersonAndDNumber("300631044141"))
    }

    @Test
    fun shouldAssertToTrueValidFNR() {
        assertTrue(validatePersonAndDNumber("30063104414"))
    }

    @Test
    fun shouldAssertToTrueValidDNR() {
        assertTrue(validatePersonAndDNumber("45069800525"))
    }

    @Test
    fun shouldFailWhenChecksum1Fails() {
        assertFalse(validatePersonAndDNumber("30063104424"))
    }

    @Test
    fun shouldFailWhenChecksum2Fails() {
        assertFalse(validatePersonAndDNumber("30063104415"))
    }

    @Test
    fun shouldFailWhenChecksum1FinalIs10() {
        assertFalse(validatePersonAndDNumber("30083104414"))
    }

}
