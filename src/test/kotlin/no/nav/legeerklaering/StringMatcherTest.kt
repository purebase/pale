package no.nav.legeerklaering

import org.junit.Assert.*
import org.junit.Test

class StringMatcherTest {

    @Test
    fun validateDifferentStringsLessThen100() {
        val percentageMatch = calculatePercentageStringMatch("abcdef", "abbdef")
        println(percentageMatch)
        assertTrue(percentageMatch < 1.0)
    }

    @Test
    fun validateTwoSimilarNamesHasMoreThen70() {
        val percentageMatch = calculatePercentageStringMatch("Oslo Legekontor AS", "Oslo Legekontor")
        println(percentageMatch)
        assertTrue(percentageMatch > 0.7)
    }

    @Test
    fun validateTwoDifferentNamesHasLessThen70() {
        val percentageMatch = calculatePercentageStringMatch("Bergen Legekontor", "Oslo Legekontor")
        println(percentageMatch)
        assertFalse(percentageMatch > 0.7)
    }
}
