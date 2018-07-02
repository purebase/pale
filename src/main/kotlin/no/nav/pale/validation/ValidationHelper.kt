package no.nav.pale.validation

import no.nav.pale.metrics.APPREC_ERROR_COUNTER
import no.nav.pale.metrics.RULE_COUNTER
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.model.msghead.Ident
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")
val personIndividualDigitsBetween1900to1999 = IntRange(0,499)
val personIndividualDigitsBetween1854to1899 = IntRange(500,749)
val personIndividualDigitsBetween2000to2039 = IntRange(500,999)
val personIndividualDigitsBetween1940to1999 = IntRange(900,999)

fun extractBornDate(personIdent: String): LocalDate =
        LocalDate.parse(personIdent.substring(0, 4).let {
            if (isDNR(personIdent)) {
                (it[0] - 4) + it.substring(1)
            } else {
                it
            }
        } + findbornyear(personIdent), personNumberDateFormat)


fun findbornyear(personIdent: String): String {
    var bornyear = ""
    val lastTwoDigistOfyear = extractLastTwoDigistOfyear(personIdent)
    val indicidualDigist = extractIndividualDigits(personIdent)
    if (personIndividualDigitsBetween1900to1999.contains(indicidualDigist.toInt())) {
        bornyear+= "19$lastTwoDigistOfyear"
    }
    else if (personIndividualDigitsBetween1854to1899.contains(indicidualDigist.toInt()) &&
            IntRange(54,99).contains(lastTwoDigistOfyear.toInt())) {
        bornyear+= "18$lastTwoDigistOfyear"
    }
    else if (personIndividualDigitsBetween2000to2039.contains(indicidualDigist.toInt()) &&
            IntRange(0,39).contains(lastTwoDigistOfyear.toInt())) {
        bornyear+= "20$lastTwoDigistOfyear"
    }
    else if (personIndividualDigitsBetween1940to1999.contains(indicidualDigist.toInt()) &&
            IntRange(40,99).contains(lastTwoDigistOfyear.toInt())) {
        bornyear+= "19$lastTwoDigistOfyear"
    }
    return bornyear
}


fun extractIndividualDigits(personIdent: String): String =
        personIdent.substring(6, 9)

fun extractLastTwoDigistOfyear(personIdent: String): String =
        personIdent.substring(4, 6)


fun collectFlowStatistics(outcomes: List<Outcome>) {
    outcomes.forEach {
        RULE_COUNTER.labels(it.outcomeType.name).inc()
        if (it.outcomeType.messagePriority == Priority.RETUR) {
            APPREC_ERROR_COUNTER.labels(it.apprecError?.dn ?: "Missing").inc()
        }
    }
}

fun extractDoctorIdentFromSender(fellesformat: EIFellesformat): Ident? =
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.find {
            it.typeId.v == "FNR"
        } ?: fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.find {
            it.typeId.v == "DNR"
        }

fun extractDoctorIdentFromSignature(fellesformat: EIFellesformat): String =
        fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur

fun extractSenderOrganisationName(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation.organisationName

fun extractPersonIdent(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.fodselsnummer

fun extractPatientSurname(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.etternavn

fun extractPatientFirstName(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.fornavn

fun isDNR(personIdent: String): Boolean =
    personIdent[0] > '3'

fun findDoctorInRelations(patient: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person, doctorPersonnumber: String): Familierelasjon? =
        patient.harFraRolleI.find {
            val aktoer = it.tilPerson.aktoer
            aktoer is PersonIdent && aktoer.ident.type.value == "FNR" && aktoer.ident.ident == doctorPersonnumber
        }

fun extractLegeerklaering(fellesformat: EIFellesformat): Legeerklaring =
        fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

fun extractSignatureDate(fellesformat: EIFellesformat): LocalDateTime =
        fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime()

fun extractOrganisationNumberFromSender(fellesformat: EIFellesformat): Ident? =
        fellesformat.msgHead.msgInfo.sender.organisation.ident.find {
            it.typeId.v == "ENH"
        }