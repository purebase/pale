package no.nav.pale.validation

import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.createFamilyRelation
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.utils.assertOutcomesContain
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PostTPSRuleFlowTest {
    @Test
    fun shouldCreateOutcomeTypeRegisterteDodITPS() {
        val person = defaultPerson()
                .withDoedsdato(Doedsdato().withDoedsdato(datatypeFactory.newXMLGregorianCalendar()))

        val outcomeList = postTPSFlow(defaultFellesformat(person), person)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.REGISTERED_DEAD_IN_TPS }

        assertEquals(OutcomeType.REGISTERED_DEAD_IN_TPS, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeGiftMedPasient() {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("EKTE", doctor)))

        assertOutcomesContain(OutcomeType.MARRIED_TO_PATIENT,
                postTPSFlow(defaultFellesformat(patient, doctor), patient))
    }

    @Test
    fun shouldCreateOutcomeTypeRegistertPartnerMedPasient() {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("REPA", doctor)))

        assertOutcomesContain(OutcomeType.REGISTERED_PARTNER_WITH_PATIENT,
                postTPSFlow(defaultFellesformat(patient, doctor), patient))
    }

    @Test
    fun testMotherIsPatientsDoctor() {
        val mother = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("MORA", mother)))
        val fellesformat = defaultFellesformat(person = patient, doctor = mother)

        assertOutcomesContain(OutcomeType.PARENT_TO_PATIENT, postTPSFlow(fellesformat, patient))
    }

    @Test
    fun testFatherIsPatientsDoctor() {
        val father = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("FARA", father)))
        val fellesformat = defaultFellesformat(person = patient, doctor = father)

        assertOutcomesContain(OutcomeType.PARENT_TO_PATIENT, postTPSFlow(fellesformat, patient))
    }

    @Test
    fun testChildIsDoctorIsPatientsChild() {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("BARN", doctor)))
        val fellesformat = defaultFellesformat(patient, doctor = doctor)

        assertOutcomesContain(OutcomeType.CHILD_OF_PATIENT, postTPSFlow(fellesformat, patient))
    }

    @Test
    fun shouldCreateRuntimeExceptionWhenPersonHarFraRollItilRolleValueIsNull() {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation(null, doctor)))
        val fellesformat = defaultFellesformat(patient, doctor = doctor)

        try {
            postTPSFlow(fellesformat, patient)
            fail("Null as role should cause exception")
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }

    @Test
    fun shouldCreateOutcomeTypePatientEmigrated() {
        val patient = defaultPerson()
                .withPersonstatus(Personstatus().withPersonstatus(Personstatuser().withValue("UTVA")))
        val fellesformat = defaultFellesformat(patient)

        assertOutcomesContain(OutcomeType.PATIENT_EMIGRATED,
                postTPSFlow(fellesformat, patient))
    }
}
