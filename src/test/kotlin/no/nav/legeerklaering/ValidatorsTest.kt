package no.nav.legeerklaering


import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun shouldFailWhePersonNumberIsMoreThan11characters() {
        assertFalse(validatePersonAndDNumber("300631044141"))
    }

    @Test
    fun shouldAssertToTrueValidFNR() {
        assertTrue(validatePersonAndDNumber("30063104414"))
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
