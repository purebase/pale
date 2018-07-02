package no.nav.pale.validation

import no.nav.pale.mapping.ApprecError
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationFlowTest {

    @Test
    fun shouldCreatePatientPersonNumberNotFound() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPasientFodselsnummerIkkeFunnet.xml")

        val outcomeList = validationFlow(fellesformat)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND }

        Assert.assertEquals(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomePatientPersonNumberNot11Digits() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPersonnummerIkke11Tegn.xml")
        val name = extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                " " + extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn +
                " " + extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn
        val fnr = extractPersonIdent(extractLegeerklaering(fellesformat))
        val fnrlengt = extractPersonIdent(extractLegeerklaering(fellesformat))!!.length

        val excpectederknadsTekst = "$name sitt fødselsnummer eller D-nummer $fnr er ikke 11 tegn. Det er $fnrlengt tegn langt."

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.PERSON_NUMBER_NOT_11_DIGITS }

        assertEquals(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomeDoctorPersonNumberNot11Digits() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPersonnummerIkke11Tegn.xml")
        val name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName +
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName
        val fnr = extractDoctorIdentFromSender(fellesformat)?.id
        val personNumberLength = extractDoctorIdentFromSender(fellesformat)!!.id.length

        val excpectederknadsTekst = "$name sitt fødselsnummer eller D-nummer $fnr er ikke 11 tegn. Det er $personNumberLength tegn langt."

        val outcome = validationFlow(fellesformat).findLast { it.outcomeType == OutcomeType.PERSON_NUMBER_NOT_11_DIGITS }

        assertEquals(OutcomeType.PERSON_NUMBER_NOT_11_DIGITS, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomePasientInvaldigPersonOrDnummber() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringInvalidFodselsnumer.xml")
        val name = extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.etternavn +
                " " + extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.fornavn +
                " " + extractLegeerklaering(fellesformat).pasientopplysninger.pasient.navn.mellomnavn
        val fnr = extractPersonIdent(extractLegeerklaering(fellesformat))

        val excpectederknadsTekst = "Fødselsnummeret eller D-nummeret $name til $fnr er feil."

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER }

        assertEquals(OutcomeType.INVALID_PERSON_NUMBER_OR_D_NUMBER, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateOutcomeDoctorInvaldigPersonOrDnummber() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringInvalidFodselsnumer.xml")
        val name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName +
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName
        val fnr = extractDoctorIdentFromSender(fellesformat)?.id

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

        val messagerecived = fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
        val messagesign = fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
        val excpectederknadsTekst = "Melding mottatt til behandling i dag $messagerecived er signert med dato $messagesign, og avvises"

        val outcome = validationFlow(fellesformat).find { it.outcomeType == OutcomeType.SIGNATURE_TOO_NEW }

        assertEquals(OutcomeType.SIGNATURE_TOO_NEW, outcome?.outcomeType)
        assertEquals(excpectederknadsTekst, outcome?.formattedMessage)
    }

    @Test
    fun shouldCreateApprecErrorPatientNameIsNotInSchema() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPatientFirstNameMissing.xml")

        val outcome = validationFlow(fellesformat).find { it.apprecError == ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA }

        assertEquals(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA, outcome?.apprecError)
    }

    @Test
    fun shouldCreateApprecErrorGenDateError() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringDoctorSignatureSchemaToNew.xml")

        val outcome = validationFlow(fellesformat).find { it.apprecError == ApprecError.GEN_DATE_ERROR }

        assertEquals(ApprecError.GEN_DATE_ERROR, outcome?.apprecError)
    }

    @Test
    fun shouldCreateApprecErrorBehandlerPersonNumberNotValid() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringInvalidFodselsnumer.xml")

        val outcome = validationFlow(fellesformat).find { it.apprecError == ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID }

        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID, outcome?.apprecError)
    }

    @Test
    fun shouldCreateApprecErrorPatientLastnameIsNotInSchema() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringPatientSurnameMissing.xml")

        val outcome = validationFlow(fellesformat).find { it.apprecError == ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA }

        assertEquals(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA, outcome?.apprecError)
    }

    @Test
    fun shouldCreateApprecErrorPatientPersonNumberIsWrong() {
        val fellesformat = readToFellesformat("/validation/legeerklaeringInvalidFodselsnumer.xml")

        val outcome = validationFlow(fellesformat).find { it.apprecError == ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG }

        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG, outcome?.apprecError)
    }

}
