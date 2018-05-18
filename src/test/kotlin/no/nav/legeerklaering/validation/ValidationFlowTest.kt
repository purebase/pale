package no.nav.legeerklaering.validation

import no.nav.legeerklaering.readToFellesformat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationFlowTest{

    @Test
    fun shouldCreatePatientPersonNumberNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientFodselsnummerIkkeFunnet.xml")

        val outcomeList = validationFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND }

        Assert.assertEquals(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, outcome?.outcomeType)

    }

    @Test
    fun shouldCreateOutcomePersonNumberNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringFodselsnummerEllerDnummerIkkeFunnet.xml")
        val name = "Inga Fos Valda"
        val excpectederknadsTekst = "Fødselsnummeret eller D-nummeret til $name finnes ikke i skjemaet."

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.PERSON_NUMBER_NOT_FOUND }

        assertEquals(OutcomeType.PERSON_NUMBER_NOT_FOUND, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomePatientPersonNumberNot11Digits() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPersonnummerIkke11Tegn.xml")
        val name = extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn +
                " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn
        val fnr = extractPersonNumber(extractLegeerklaering(fellesformat))
        val fnrlengt = extractPersonNumber(extractLegeerklaering(fellesformat)).length


        val excpectederknadsTekst = "$name sitt fødselsnummer eller D-nummer $fnr er ikke 11 tegn. Det er $fnrlengt tegn langt."


        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.PERSON_NUMBER_NOT_11_DIGITS }

        assertEquals(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomeDoctorPersonNumberNot11Digits() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPersonnummerIkke11Tegn.xml")
        val name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName
        val fnr = extractDoctorPersonNumberFromSender(fellesformat)
        val fnrlengt = extractDoctorPersonNumberFromSender(fellesformat).length


        val excpectederknadsTekst = "$name sitt fødselsnummer eller D-nummer $fnr er ikke 11 tegn. Det er $fnrlengt tegn langt."


        val outcome = validationFlow(fellesformat).findLast { it.outcomeType == OutcomeType.PERSON_NUMBER_NOT_11_DIGITS }

        assertEquals(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomePasientInvaldigPersonOrDnummber() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringInvalidFodselsnumer.xml")
        val name = extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn +
                " "+ extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn
        val fnr = extractPersonNumber(extractLegeerklaering(fellesformat))

        val excpectederknadsTekst = "Fødselsnummeret eller D-nummeret $name til $fnr er feil."


        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER }

        assertEquals(OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomeDoctorInvaldigPersonOrDnummber() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringInvalidFodselsnumer.xml")
        val name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName+
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName
        val fnr = extractDoctorPersonNumberFromSender(fellesformat)

        val excpectederknadsTekst = "Fødselsnummeret eller D-nummeret $name til $fnr er feil."


        val outcome = validationFlow(fellesformat).findLast { it.outcomeType == OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER }

        assertEquals(OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomePatientSurnameNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPatientSurnameMissing.xml")

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.PATIENT_SURNAME_NOT_FOUND }

        assertEquals(OutcomeType.PATIENT_SURNAME_NOT_FOUND, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomePatientFirstNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPatientFirstNameMissing.xml")

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND }

        assertEquals(OutcomeType.PATIENT_FIRST_NAME_NOT_FOUND, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeDoctorMissmaatchedPersonNumberSignatureSchema() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringDoctorMismatchedPersonNumberSignatureSchema.xml")

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA }

        assertEquals(OutcomeType.MISMATCHED_PERSON_NUMBER_SIGNATURE_SCHEMA, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeDoctorSignatureSchemaToNew() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringDoctorSignatureSchemaToNew.xml")

        val messagerecived =  fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
        val messagesign = fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
        val excpectederknadsTekst = "Melding mottatt til behandling i dag $messagerecived er signert med dato $messagesign, og avvises"

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.SIGNATURE_TOO_NEW }

        assertEquals(OutcomeType.SIGNATURE_TOO_NEW, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }
}
