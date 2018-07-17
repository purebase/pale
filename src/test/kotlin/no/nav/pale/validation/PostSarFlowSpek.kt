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
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostSarFlowSpek : Spek({
    describe("No samhandler returned") {
        val fellesformat = defaultFellesformat(person = defaultPerson())
        it("Should create outcome missing in SAR") {
            assertOutcomesContain(OutcomeType.BEHANDLER_NOT_SAR,
                    postSARFlow(fellesformat, listOf()))
        }
    }

    describe("No address registerred in SAR") {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis(addressLine1 = null)
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))
        it("Should create outcome address missing in SAR") {
            assertOutcomesContain(OutcomeType.ADDRESS_MISSING_SAR,
                    postSARFlow(fellesformat, samhandlerList))
        }
    }

    describe("Samhandler praktis is emergancy room") {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis(praksisTypeKode = "LEVA")
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))
        it("Should create outcome for praksis being a emergancy room") {
            assertOutcomesContain(OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM,
                    postSARFlow(fellesformat, samhandlerList))
        }
    }

    describe("Doctor uses DNR but has FNR in SAR") {
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

        it("Should create outcome for missmatched ident") {

            assertOutcomesContain(OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR,
                    postSARFlow(fellesformat, samhandlerList))
        }
    }

    describe("No valid samhandler praksis type code") {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(
                samhandlerPraksisListe = listOf(samhandlerPraksis),
                samhandlerTypeKode = "FT")
        )
        it("Should add outcome for no valid practice types in SAR") {
            assertOutcomesContain(OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR,
                    postSARFlow(fellesformat, samhandlerList))
        }
    }

    describe("Uncertain SAR response") {
        val doctor = defaultPerson()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(doctor.defaultSamhandlerPraksis(name = "THISSHOULDNOTEXIST"))))

        it("Should add outcome for verifying sar response") {
            assertOutcomesContain(OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED,
                    postSARFlow(fellesformat, samhandlerList))
        }
    }

    describe("Finding a good match for samhandler praksis should not add uncertain resposne") {
        val doctor = defaultPerson()
        val samhandlerPraksis = doctor.defaultSamhandlerPraksis()
        val fellesformat = defaultFellesformat(
                person = defaultPerson(),
                doctor = doctor,
                samhandlerPraksis = samhandlerPraksis
        )
        val samhandlerList = listOf(doctor.toSamhandler(samhandlerPraksisListe = listOf(samhandlerPraksis)))
        it("Should not add outcome for verifying sar response") {
            assertOutcomesNotContain(OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED,
                    postSARFlow(fellesformat, samhandlerList))
        }
    }
})
