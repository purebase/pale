package no.nav.pale.validation

import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.utils.assertOutcomesContain
import org.junit.Test

class PreTPSRuleFlowTest {

    @Test
    fun shouldCreateOutcomeTypePasientOver70() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE))
        )
        assertOutcomesContain(OutcomeType.PATIENT_IS_OVER_70, preTPSFlow(fellesformat))
    }

    @Test
    fun shouldCreateOutcomeTypePasientOver70Dnummer() {
        val fellesformat = defaultFellesformat(
                person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE), useDNumber = true)
        )
        assertOutcomesContain(OutcomeType.PATIENT_IS_OVER_70, preTPSFlow(fellesformat))
    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerErPasient() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(
                person = person,
                doctor = person
        )
        assertOutcomesContain(OutcomeType.BEHANDLER_IS_PATIENT, preTPSFlow(fellesformat))
    }
}
