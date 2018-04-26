package no.nav.legeerklaering.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

fun extractDoctorPersonNumber(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.find {
    it.typeId.v == "FNR"
}!!.id

fun extractSenderOrganisationNumber(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation.ident.find {
    it.typeId.v == "ENH"
}!!.id

fun extractPersonNumber(legeerklaering: Legeerklaring): String
        = legeerklaering.pasientopplysninger.pasient.fodselsnummer

fun extractBornDate(personNumber: String): LocalDate =
        LocalDate.parse(personNumber.substring(0, 6), formatter)

fun findDoctorInRelations(patient: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person, doctorPersonnumber: String): Familierelasjon? =
        patient.harFraRolleI.find {
            val aktoer = it.tilPerson.aktoer
            aktoer is PersonIdent && aktoer.ident.type.value == "FNR" && aktoer.ident.ident == doctorPersonnumber
        }
