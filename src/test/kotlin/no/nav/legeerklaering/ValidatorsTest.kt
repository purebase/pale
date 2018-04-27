package no.nav.legeerklaering


import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun shouldFailWhePersonNumberIsMoreThan11characters() {
        assertFalse(validatePersonDNumberMod11("300631044141"))
    }

    @Test
    fun shouldAssertToTrueValidFNR() {
        assertTrue(validatePersonDNumberMod11("30063104414"))
    }

    @Test
    fun shouldFailWhenChecksum1Fails() {
        assertFalse(validatePersonDNumberMod11("30063104424"))
    }

    @Test
    fun shouldFailWhenChecksum2Fails() {
        assertFalse(validatePersonDNumberMod11("30063104415"))
    }

    @Test
    fun shouldFailWhenChecksum1FinalIs10() {
        assertFalse(validatePersonDNumberMod11("30083104414"))
    }

}
