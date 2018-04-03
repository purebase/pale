package no.nav.legeerklaering


import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun shouldFailWhePersonNumberIsMoreThan11characters() {
        assertFalse(validatePersonNumber("300631044141"))
    }

    @Test
    fun shouldAssertToTrueValidFNR() {
        assertTrue(validatePersonNumber("30063104414"))
    }

    @Test
    fun shouldFailWhenChecksum1Fails() {
        assertFalse(validatePersonNumber("30063104424"))
    }

    @Test
    fun shouldFailWhenChecksum2Fails() {
        assertFalse(validatePersonNumber("30063104415"))
    }

    @Test
    fun shouldFailWhenChecksum1FinalIs10() {
        assertFalse(validatePersonNumber("30083104414"))
    }

}