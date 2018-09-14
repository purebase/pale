package no.nav.pale.validation

import no.nav.pale.datagen.datatypeFactory
import no.nav.pale.datagen.createFamilyRelation
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.utils.shouldContainOutcome
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import org.amshove.kluent.shouldThrow
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostTPSRuleFlowSpek : Spek({
    describe("Person is registered as dead in TPS") {
        val person = defaultPerson()
                .withDoedsdato(Doedsdato().withDoedsdato(datatypeFactory.newXMLGregorianCalendar()))
        it("Creates outcome for being registered as dead in TPS") {
            postTPSFlow(defaultFellesformat(person), person) shouldContainOutcome OutcomeType.REGISTERED_DEAD_IN_TPS
        }
    }
    describe("Doctor is married to patient") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("EKTE", doctor)))
        it("Creates outcome for doctor being married to patient") {
            postTPSFlow(defaultFellesformat(patient, doctor), patient) shouldContainOutcome OutcomeType.MARRIED_TO_PATIENT
        }
    }
    describe("Doctor is registered partner with patient") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("REPA", doctor)))
        it("Creates outcome for being registered partner with patient") {
            postTPSFlow(defaultFellesformat(patient, doctor), patient) shouldContainOutcome OutcomeType.REGISTERED_PARTNER_WITH_PATIENT
        }
    }
    describe("Mother is patient to doctor") {
        val mother = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("MORA", mother)))
        it("Creates outcome for being parent to patient") {
            postTPSFlow(defaultFellesformat(patient, mother), patient) shouldContainOutcome OutcomeType.PARENT_TO_PATIENT
        }
    }
    describe("Father is patient to doctor") {
        val father = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("FARA", father)))
        it("Creates outcome for being parent to patient") {
            postTPSFlow(defaultFellesformat(person = patient, doctor = father), patient) shouldContainOutcome OutcomeType.PARENT_TO_PATIENT
        }
    }
    describe("Doctor is child of patient") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("BARN", doctor)))
        it("Creates outcome for being child of patient") {
            postTPSFlow(defaultFellesformat(patient, doctor), patient) shouldContainOutcome OutcomeType.CHILD_OF_PATIENT
        }
    }
    describe("Patient has a null family relation") {
        val doctor = defaultPerson()
        val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation(null, doctor)))
        val fellesformat = defaultFellesformat(patient, doctor)
        it("Creates an exception") {
            { postTPSFlow(fellesformat, patient) } shouldThrow RuntimeException::class
        }
    }
    describe("Patient is registered as emigrated in TPS") {
        val patient = defaultPerson()
                .withPersonstatus(Personstatus().withPersonstatus(Personstatuser().withValue("UTVA")))
        it("Creates the outcome for having emigrated") {
            postTPSFlow(defaultFellesformat(patient), patient) shouldContainOutcome OutcomeType.PATIENT_EMIGRATED
        }
    }
    describe("Patient is registered as SPSF in TPS") {
        val doctor = defaultPerson()
        val patient = defaultPerson().withDiskresjonskode(Diskresjonskoder().withValue("SPSF"))
        it("Creates the outcome for code 6 person") {
            postTPSFlow(defaultFellesformat(patient), patient) shouldContainOutcome OutcomeType.PATIENT_HAS_SPERREKODE_6
        }
    }
    describe("Patient is registered as SPFO in TPS") {
        val doctor = defaultPerson()
        val patient = defaultPerson().withDiskresjonskode(Diskresjonskoder().withValue("SPFO"))
        it("Creates the outcome for code 7 person") {
            postTPSFlow(defaultFellesformat(patient), patient) shouldContainOutcome OutcomeType.PATIENT_HAS_SPERREKODE_7
        }
    }
})
