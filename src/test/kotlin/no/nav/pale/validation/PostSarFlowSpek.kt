package no.nav.pale.validation

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.defaultSamhandlerPraksis
import no.nav.pale.datagen.generateAktoer
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.datagen.ident
import no.nav.pale.datagen.toSamhandler
import no.nav.pale.utils.shouldContainOutcome
import no.nav.pale.utils.shouldNotContainOutcome
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostSarFlowSpek : Spek({
    describe("No samhandler returned") {
        val fellesformat = defaultFellesformat(person = defaultPerson())
        it("Creates outcome missing in SAR") {
            postSARFlow(fellesformat, listOf()) shouldContainOutcome OutcomeType.BEHANDLER_NOT_SAR
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
        it("Creates outcome address missing in SAR") {
            postSARFlow(fellesformat, samhandlerList) shouldContainOutcome OutcomeType.ADDRESS_MISSING_SAR
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
        it("Creates outcome for praksis being a emergancy room") {
            postSARFlow(fellesformat, samhandlerList) shouldContainOutcome OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
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

        it("Creates outcome for missmatched ident") {
            postSARFlow(fellesformat, samhandlerList) shouldContainOutcome OutcomeType.BEHANDLER_HAS_FNR_USES_DNR
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
        it("Creates for no valid practice types in SAR") {
            postSARFlow(fellesformat, samhandlerList) shouldContainOutcome OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR
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

        it("Creates outcome for  uncertain SAR response") {
            postSARFlow(fellesformat, samhandlerList) shouldContainOutcome OutcomeType.UNCERTAIN_RESPONSE_SAR
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
        it("Does not add outcome for verifying sar response") {
            postSARFlow(fellesformat, samhandlerList) shouldNotContainOutcome OutcomeType.UNCERTAIN_RESPONSE_SAR
        }
    }
})
