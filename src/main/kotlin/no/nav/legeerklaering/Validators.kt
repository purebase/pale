package no.nav.legeerklaering

val lookup1: IntArray = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2, 0)
val lookup2: IntArray = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

private fun validatePersonDNumberMod11(personNumber: String): Boolean {
    if (personNumber.length != 11)
        return false

    var checksum1 = 0
    var checksum2 = 0

    for (i in 0..9) {
        val currNum = personNumber[i]-'0'
        checksum1 += currNum * lookup1[i]
        checksum2 += currNum * lookup2[i]
    }

    checksum1 %= 11
    checksum2 %= 11

    val checksum1Final = if (checksum1 == 0) { 0 } else { 11 - checksum1 }
    val checksum2Final = if (checksum2 == 0) { 0 } else { 11 - checksum2 }

    if (checksum1Final == 10)
        return false

    return personNumber[9]-'0' == checksum1Final && personNumber[10]-'0' == checksum2Final
}

private fun validatePersonDNumberRange(personNumber: String): Boolean {
    throw RuntimeException("Not implemented")
}

fun validatePersonAndDNumber(personNumber: String): Boolean =
        validatePersonDNumberMod11(personNumber) && validatePersonDNumberRange(personNumber)
