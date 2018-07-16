package no.nav.pale.validation

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.defaultSamhandlerPraksis
import no.nav.pale.datagen.generateAktoer
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.datagen.ident
import no.nav.pale.datagen.toSamhandler
import no.nav.pale.utils.assertOutcomesContain
import no.nav.pale.utils.assertOutcomesNotContain
import org.junit.Test

class PostSarFlowTest {
    @Test
    fun testCreatesOutcomeWhenSamhandlerNotFound() {
        val fellesformat = defaultFellesformat(person = defaultPerson())
        assertOutcomesContain(OutcomeType.BEHANDLER_NOT_SAR,
                postSARFlow(fellesformat, listOf()))
    }

    @Test
    fun testCreatesOutcomeOnMissingAddress() {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis(addressLine1 = null)
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))
        assertOutcomesContain(OutcomeType.ADDRESS_MISSING_SAR,
                postSARFlow(fellesformat, samhandlerList))
    }

    @Test
    fun testCreatesOutcomeOnLegevakt() {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis(praksisTypeKode = "LEVA")
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))
        assertOutcomesContain(OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM,
                postSARFlow(fellesformat, samhandlerList))
    }

    @Test
    fun testCreatesOutcomeDNRInFellesformatButHasFNRInSAR() {
        val doctor = defaultPerson(useDNumber = true)
        val doctorFNR = generatePersonNumber(extractBornDate(doctor.ident()))
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        doctor.aktoer = generateAktoer(extractBornDate(doctorFNR), useDNumber = false)

        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))
        assertOutcomesContain(OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR,
                postSARFlow(fellesformat, samhandlerList))
    }

    @Test
    fun testReturnsOutcomeOnNoValidSamhandlerPraksisTypeKode() {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis),
                samhandlerTypeKode = "FT"))
        assertOutcomesContain(OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR,
                postSARFlow(fellesformat, samhandlerList))
    }

    @Test
    fun testReturnsOutcomeOnUncertainSarResponse() {
        val doctor = defaultPerson()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(doctor.defaultSamhandlerPraksis(name = "THISSHOULDNOTEXIST"))))

        assertOutcomesContain(OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED,
                postSARFlow(fellesformat, samhandlerList))
    }

    @Test
    fun testDoesNotContainOutcomeWhenMatchingOver90Percentage() {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))

        assertOutcomesNotContain(OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED,
                postSARFlow(fellesformat, samhandlerList))
    }
}
