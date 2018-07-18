package no.nav.pale.validation

import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.utils.shouldContainOutcome
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PreTPSRuleFlowSpek : Spek({
    describe("Patient is over 70 years old") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE))
        )
        it("Creates outcome for patient being over 70 years old") {
            preTPSFlow(fellesformat) shouldContainOutcome OutcomeType.PATIENT_IS_OVER_70
        }
    }
    describe("Patient is over 70 years old and have a DNR") {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE), useDNumber = true)
        )
        it("Creates outcome for patient being over 70 years old") {
            preTPSFlow(fellesformat) shouldContainOutcome OutcomeType.PATIENT_IS_OVER_70
        }
    }
    describe("Doctor is patient") {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(
                person = person,
                doctor = person
        )
        it("Creates outcome for doctor being the patient") {
            preTPSFlow(fellesformat) shouldContainOutcome OutcomeType.BEHANDLER_IS_PATIENT
        }
    }
})
