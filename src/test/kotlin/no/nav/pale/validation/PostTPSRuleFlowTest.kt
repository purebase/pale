package no.nav.pale.validation

import no.nav.pale.utils.readToFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

class PostTPSRuleFlowTest {

    val fellesformat = readToFellesformat("/legeerklaering.xml")

    @Test
    fun shouldCreateOutcomeTypeRegisterteDodITPS() {
        val person = Person().apply {
            doedsdato = Doedsdato().apply {
                setDoedsdato(DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar()))
            }
        }

        val outcomeList = postTPSFlow(fellesformat, person)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.REGISTERED_DEAD_IN_TPS }

        assertEquals(OutcomeType.REGISTERED_DEAD_IN_TPS, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeGiftMedPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("EKTE")
            harFraRolleI.add(familierelasjon)
        }

        val outcomeList = postTPSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.MARRIED_TO_PATIENT }

        assertEquals(OutcomeType.MARRIED_TO_PATIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeSamboerMedPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("SAMB")
            harFraRolleI.add(familierelasjon)
        }

        val outcomeList = postTPSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.COHABITANT_WITH_PATIENT }

        assertEquals(OutcomeType.COHABITANT_WITH_PATIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeRegistertPartnerMedPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("REPA")
            harFraRolleI.add(familierelasjon)
        }

        val outcomeList = postTPSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.REGISTERED_PARTNER_WITH_PATIENT }

        assertEquals(OutcomeType.REGISTERED_PARTNER_WITH_PATIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeForelderTilPasient() {

        val patientFARA = Person().apply {
            val familierelasjon = familierelasjon("FARA")
            harFraRolleI.add(familierelasjon)
        }

        val patientMORA = Person().apply {
            val familierelasjon = familierelasjon("MORA")
            harFraRolleI.add(familierelasjon)
        }

        val outcomeListFARA = postTPSFlow(fellesformat, patientFARA)
        val outcomeListMORA = postTPSFlow(fellesformat, patientMORA)
        val outcomeFARA = outcomeListFARA.find { it.outcomeType == OutcomeType.PARENT_TO_PATIENT }
        val outcomeMORA = outcomeListMORA.find { it.outcomeType == OutcomeType.PARENT_TO_PATIENT }

        assertEquals(OutcomeType.PARENT_TO_PATIENT, outcomeFARA?.outcomeType)
        assertEquals(OutcomeType.PARENT_TO_PATIENT, outcomeMORA?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeBarnAvPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("BARN")
            harFraRolleI.add(familierelasjon)
        }

        val outcomeList = postTPSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.CHILD_OF_PATIENT }

        assertEquals(OutcomeType.CHILD_OF_PATIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeGiftLeverAdskilt() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("GLAD")
            harFraRolleI.add(familierelasjon)
        }

        val outcomeList = postTPSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.MARIED_LIVES_SEPERATED }

        assertEquals(OutcomeType.MARIED_LIVES_SEPERATED, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateRuntimeExceptionWhenPersonHarFraRollItilRolleValueIsNull() {

        val patient = Person().apply {
            val familierelasjon = Familierelasjon().apply {
                tilRolle = Familierelasjoner().apply {
                    value = null
                }
                tilPerson = Person().apply {

                    val doctorIdent = extractDoctorIdentFromSender(fellesformat)!!
                    aktoer = PersonIdent().apply {
                        ident = NorskIdent().apply {
                            ident = doctorIdent.id
                            type = Personidenter().apply {
                                value = doctorIdent.typeId.v
                            }
                        }
                    }
                }
            }
            harFraRolleI.add(familierelasjon)
        }

        try {
            postTPSFlow(fellesformat, patient)
        } catch (e: RuntimeException) {
            assertEquals("relations.tilRolle.value must not be null", e.message)
        }
    }

    @Test
    fun shouldCreateOutcomeTypePatientEmigrated() {

        val patient = Person().apply {
            personstatus = Personstatus().apply {
                personstatus = Personstatuser().apply {
                    value = "UTVA"
                }
            }
        }
        val outcomeList = postTPSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_EMIGRATED }

        assertEquals(OutcomeType.PATIENT_EMIGRATED, outcome?.outcomeType)
    }

    fun familierelasjon(faimilierelasjon: String): Familierelasjon = Familierelasjon().apply {
        tilRolle = Familierelasjoner().apply {
            value = faimilierelasjon
        }
        tilPerson = Person().apply {

            aktoer = PersonIdent().apply {
                ident = NorskIdent().apply {
                    val doctorIdent = extractDoctorIdentFromSender(fellesformat)!!
                    ident = doctorIdent.id
                    type = Personidenter().apply {
                        value = doctorIdent.typeId.v
                    }
                }
            }
        }
    }
}
