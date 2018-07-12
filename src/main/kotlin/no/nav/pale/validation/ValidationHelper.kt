package no.nav.pale.validation

import no.nav.pale.metrics.APPREC_ERROR_COUNTER
import no.nav.pale.metrics.RULE_COUNTER
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.model.msghead.Ident
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

fun extractBornDate(personIdent: String): LocalDate =
        LocalDate.of(extractBornYear(personIdent), extractBornMonth(personIdent), extractBornDay(personIdent))

fun extractBornYear(personIdent: String): Int {
    val lastTwoDigitsOfYear = extractLastTwoDigistOfyear(personIdent)
    val individualDigits = extractIndividualDigits(personIdent)
    if (individualDigits in (0..499)) {
        return 1900 + lastTwoDigitsOfYear
    }

    if (individualDigits in 500..749) {
        return 1800 + lastTwoDigitsOfYear
    }

    if (individualDigits in 500..999) {
        return 2000 + lastTwoDigitsOfYear
    }
    return 1900 + lastTwoDigitsOfYear
}

fun extractBornDay(personIdent: String): Int {
    val day = personIdent.substring(0..1).toInt()
    return if (day < 40) day else day - 40
}

fun extractBornMonth(personIdent: String): Int = personIdent.substring(2..3).toInt()

fun extractIndividualDigits(personIdent: String): Int = personIdent.substring(6, 9).toInt()

fun extractLastTwoDigistOfyear(personIdent: String): Int = personIdent.substring(4, 6).toInt()

fun collectFlowStatistics(outcomes: List<Outcome>) {
    outcomes.forEach {
        RULE_COUNTER.labels(it.outcomeType.name).inc()
        if (it.outcomeType.messagePriority == Priority.RETUR) {
            APPREC_ERROR_COUNTER.labels(it.apprecError?.dn ?: "Missing").inc()
        }
    }
}

fun extractDoctorIdentFromSender(fellesformat: EIFellesformat): Ident? =
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional?.ident?.find {
            it.typeId.v == "FNR"
        } ?: fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional?.ident?.find {
            it.typeId.v == "DNR"
        }

fun extractDoctorIdentFromSignature(fellesformat: EIFellesformat): String =
        fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur

fun extractSenderOrganisationName(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation?.organisationName ?: ""

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
            aktoer is PersonIdent /*&& aktoer.ident.type.value == "FNR" */&& aktoer.ident.ident == doctorPersonnumber
        }

fun extractLegeerklaering(fellesformat: EIFellesformat): Legeerklaring =
        fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

fun extractSignatureDate(fellesformat: EIFellesformat): ZonedDateTime =
        fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime()

fun extractReceivedDate(fellesformat: EIFellesformat): ZonedDateTime =
        fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime()

fun extractOrganisationNumberFromSender(fellesformat: EIFellesformat): Ident? =
        fellesformat.msgHead.msgInfo.sender.organisation.ident.find {
            it.typeId.v == "ENH"
        }

val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
fun format(date: TemporalAccessor): String = dateFormat.format(date)
