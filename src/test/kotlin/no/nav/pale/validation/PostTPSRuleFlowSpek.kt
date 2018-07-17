package no.nav.pale.validation

import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.createFamilyRelation
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.utils.assertOutcomesContain
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import org.junit.Assert.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostTPSRuleFlowSpek : Spek({
    describe("Person is registered as dead in TPS") {
        val person = defaultPerson()
                .withDoedsdato(Doedsdato().withDoedsdato(datatypeFactory.newXMLGregorianCalendar()))
        it("Should contain outcome for being registered as dead in TPS") {
            assertOutcomesContain(OutcomeType.REGISTERED_DEAD_IN_TPS,
                    postTPSFlow(defaultFellesformat(person), person))
        }
    }
    describe("Doctor is married to patient") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("EKTE", doctor)))
        it("Should create outcome for doctor being married to patient") {
            assertOutcomesContain(OutcomeType.MARRIED_TO_PATIENT,
                    postTPSFlow(defaultFellesformat(patient, doctor), patient))
        }
    }
    describe("Doctor is registered partner with patient") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("REPA", doctor)))
        it("Should create outcome for being registered partner with patient") {
            assertOutcomesContain(OutcomeType.REGISTERED_PARTNER_WITH_PATIENT,
                    postTPSFlow(defaultFellesformat(patient, doctor), patient))
        }
    }
    describe("Mother is patient to doctor") {
        val mother = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("MORA", mother)))
        it("Should contain outcome for being parent to patient") {
            assertOutcomesContain(OutcomeType.PARENT_TO_PATIENT,
                    postTPSFlow(defaultFellesformat(person = patient, doctor = mother), patient))
        }
    }
    describe("Father is patient to doctor") {
        val father = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("FARA", father)))
        it("Should contain outcome for being parent to patient") {
            assertOutcomesContain(OutcomeType.PARENT_TO_PATIENT,
                    postTPSFlow(defaultFellesformat(person = patient, doctor = father), patient))
        }
    }
    describe("Doctor is child of patient") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("BARN", doctor)))
        it("Should contain outcome for being child of patient") {
            assertOutcomesContain(OutcomeType.CHILD_OF_PATIENT,
                    postTPSFlow(defaultFellesformat(patient, doctor = doctor), patient))
        }
    }
    describe("Patient has a null family relation") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation(null, doctor)))
        val fellesformat = defaultFellesformat(patient, doctor = doctor)
        it("Should cause an exception") {
            try {
                postTPSFlow(fellesformat, patient)
                fail("Null as role should cause exception")
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }
    describe("Patient is registered as emigrated in TPS") {
        val patient = defaultPerson()
                .withPersonstatus(Personstatus().withPersonstatus(Personstatuser().withValue("UTVA")))
        it("Should contain the outcome for having emigrated") {
            assertOutcomesContain(OutcomeType.PATIENT_EMIGRATED,
                    postTPSFlow(defaultFellesformat(patient), patient))
        }
    }
})
