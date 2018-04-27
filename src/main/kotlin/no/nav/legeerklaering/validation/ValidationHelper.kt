package no.nav.legeerklaering.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

fun extractDoctorPersonNumberFromSender(fellesformat: EIFellesformat): String =
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.find {
            it.typeId.v == "FNR"
        }!!.id

fun extractDoctorPersonNumberFromSignature(fellesformat: EIFellesformat): String =
        fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur

fun extractSenderOrganisationNumber(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation.ident.find {
    it.typeId.v == "ENH"
}!!.id

fun extractPersonNumber(legeerklaering: Legeerklaring): String
        = legeerklaering.pasientopplysninger.pasient.fodselsnummer

fun extractPatientSurname(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.etternavn

fun extractPatientFirstName(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.fornavn

fun extractPatientMiddleName(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.mellomnavn

// TODO: We might need to modify this to work with DNR
fun extractBornDate(personNumber: String): LocalDate =
        LocalDate.parse(personNumber.substring(0, 6), formatter)

fun findDoctorInRelations(patient: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person, doctorPersonnumber: String): Familierelasjon? =
        patient.harFraRolleI.find {
            val aktoer = it.tilPerson.aktoer
            aktoer is PersonIdent && aktoer.ident.type.value == "FNR" && aktoer.ident.ident == doctorPersonnumber
        }

fun extractLegeerklaering(fellesformat: EIFellesformat): Legeerklaring =
        fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

fun extractSignatureDate(fellesformat: EIFellesformat): LocalDateTime =
        fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
