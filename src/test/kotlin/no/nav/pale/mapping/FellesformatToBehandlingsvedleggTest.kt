package no.nav.pale.mapping

import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.utils.readToFellesformat
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.toOutcome
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FellesformatToBehandlingsvedleggTest {

    val fellesformat = readToFellesformat("/mapping/legeerklaeringMinimumFields.xml")
    val outcomes = listOf(
            OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.toOutcome(
                    generatePersonNumber(LocalDate.now().minusYears(40)),
                    apprecError = ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA),
            OutcomeType.CHILD_OF_PATIENT.toOutcome()
    )

    @Test
    fun shouldCreateBehandlingsvedlegg() {

        try {
            mapFellesformatToBehandlingsVedlegg(fellesformat, outcomes)
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            Assert.fail()
        }
    }


    @Test
    fun shouldCreateHCPFormatedNameWithMiddelName() {
        val fellesformat = readToFellesformat("/legeerklaering.xml")
        val org = fellesformat.msgHead.msgInfo.sender.organisation
        val hcp = org.healthcareProfessional
        val formatedName = hcp.formatName()
        assertEquals("VALDA INGA FOS", formatedName)

    }

    @Test
    fun shouldCreateHCPFormatedNameWithoutMiddelName() {
        val fellesformat = readToFellesformat("/mapping/legeerklaeringHcpNoMiddelName.xml")
        val org = fellesformat.msgHead.msgInfo.sender.organisation
        val hcp = org.healthcareProfessional
        val formatedName = hcp.formatName()
        assertEquals("VALDA INGA", formatedName)

    }
}