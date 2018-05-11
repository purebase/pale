package no.nav.legeerklaering.validation

import no.nav.legeerklaering.readToFellesformat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*
import javax.xml.datatype.DatatypeFactory




class PostTSSRuleFlowTest {


    val fellesformat = readToFellesformat("/legeerklaering.xml")

    @Test
    fun shouldCreateOutcomeTypeRegisterteDodITPS() {
        val person = Person().apply {
            doedsdato = Doedsdato().apply {
                setDoedsdato(DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar()))
            }
        }

        val outcomeList = postTSSFlow(fellesformat, person)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.REGISTRERT_DOD_I_TPS }

        assertEquals(OutcomeType.REGISTRERT_DOD_I_TPS, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeGiftMedPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("EKTE")
            harFraRolleI.add(familierelasjon)

        }

        val outcomeList = postTSSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.GIFT_MED_PASIENT }

        assertEquals(OutcomeType.GIFT_MED_PASIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeSamboerMedPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("SAMB")
            harFraRolleI.add(familierelasjon)

        }

        val outcomeList = postTSSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.SAMBOER_MED_PASIENT }

        assertEquals(OutcomeType.SAMBOER_MED_PASIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeRegistertPartnerMedPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("REPA")
            harFraRolleI.add(familierelasjon)

        }

        val outcomeList = postTSSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.REGISTRERT_PARTNER_MED_PASIENT }

        assertEquals(OutcomeType.REGISTRERT_PARTNER_MED_PASIENT, outcome?.outcomeType)
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

        val outcomeListFARA = postTSSFlow(fellesformat, patientFARA)
        val outcomeListMORA = postTSSFlow(fellesformat, patientMORA)
        val outcomeFARA = outcomeListFARA.find { it.outcomeType == OutcomeType.FORELDER_TIL_PASIENT }
        val outcomeMORA = outcomeListMORA.find { it.outcomeType == OutcomeType.FORELDER_TIL_PASIENT }

        assertEquals(OutcomeType.FORELDER_TIL_PASIENT, outcomeFARA?.outcomeType)
        assertEquals(OutcomeType.FORELDER_TIL_PASIENT, outcomeMORA?.outcomeType)
    }


    @Test
    fun shouldCreateOutcomeTypeBarnAvPasient() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("BARN")
            harFraRolleI.add(familierelasjon)

        }

        val outcomeList = postTSSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BARN_AV_PASIENT }

        assertEquals(OutcomeType.BARN_AV_PASIENT, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeGiftLeverAdskilt() {

        val patient = Person().apply {
            val familierelasjon = familierelasjon("GLAD")
            harFraRolleI.add(familierelasjon)

        }

        val outcomeList = postTSSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.GIFT_LEVER_ADSKILT }

        assertEquals(OutcomeType.GIFT_LEVER_ADSKILT, outcome?.outcomeType)
    }



    @Test
    fun shouldCreateRuntimeExceptionWhenPersonHarFraRollItilRolleValueIsNull() {

        val patient = Person().apply {
            val familierelasjon = Familierelasjon().apply {
                tilRolle = Familierelasjoner().apply {
                    value = null
                }
                tilPerson = Person().apply {

                    aktoer = PersonIdent().apply {
                        ident = NorskIdent().apply {
                            ident = extractDoctorPersonNumberFromSender(fellesformat)
                            type = Personidenter().apply {
                                value = "FNR"
                            }
                        }
                    }
                }

            }
            harFraRolleI.add(familierelasjon)
        }

        try {
            postTSSFlow(fellesformat, patient)
        }
        catch (e: RuntimeException) {
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
        val outcomeList = postTSSFlow(fellesformat, patient)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_EMIGRATED }

        assertEquals(OutcomeType.PATIENT_EMIGRATED, outcome?.outcomeType)
    }


    fun familierelasjon(faimilierelasjon: String): Familierelasjon  =  Familierelasjon().apply {
        tilRolle = Familierelasjoner().apply {
            value = faimilierelasjon
        }
        tilPerson = Person().apply {

            aktoer = PersonIdent().apply {
                ident = NorskIdent().apply {
                    ident = extractDoctorPersonNumberFromSender(fellesformat)
                    type = Personidenter().apply {
                        value = "FNR"
                    }
                }
            }
        }
    }
}