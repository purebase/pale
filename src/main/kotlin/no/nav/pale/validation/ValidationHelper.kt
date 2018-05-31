package no.nav.pale.validation

import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
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

val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

data class RuleExecutionInfo<P, D>(
        val fellesformat: EIFellesformat,
        val legeerklaering: Legeerklaring,
        val patientIdent: P,
        val doctorIdent: D,
        val outcome: MutableList<Outcome>
)

fun initFlow(fellesformat: EIFellesformat): Observable<RuleExecutionInfo<String?, Ident?>> =
        listOf(fellesformat).toObservable()
                .map {
                    val legeerklaering = extractLegeerklaering(it)
                    RuleExecutionInfo(
                            fellesformat = it,
                            legeerklaering = legeerklaering,
                            patientIdent = extractPersonIdent(legeerklaering),
                            doctorIdent = extractDoctorIdentFromSender(it),
                            outcome = mutableListOf()
                    )
                }

fun collectFlowStatistics(outcomes: List<Outcome>) {
    outcomes.forEach {
        RULE_COUNTER.labels(it.outcomeType.name).inc()
        if (it.outcomeType.messagePriority == Priority.RETUR) {
            APPREC_ERROR_COUNTER.labels(it.apprecError?.v ?: "Missing").inc()
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

fun extractSenderOrganisationNumber(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation.ident.find {
    it.typeId.v == "ENH"
}!!.id

fun extractSenderOrganisationName(fellesformat: EIFellesformat): String = fellesformat.msgHead.msgInfo.sender.organisation.organisationName

fun extractPersonIdent(legeerklaering: Legeerklaring): String?
        = legeerklaering.pasientopplysninger.pasient.fodselsnummer

fun extractPatientSurname(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.etternavn

fun extractPatientFirstName(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.fornavn

fun extractPatientMiddleName(legeerklaering: Legeerklaring): String? =
        legeerklaering.pasientopplysninger.pasient.navn.mellomnavn

fun extractBornDate(personIdent: String): LocalDate =
        LocalDate.parse(personIdent.substring(0, 6).let {
            if (isDNR(personIdent)) {
                (it[0] - 3) + it.substring(1)
            } else {
                it
            }
        }, personNumberDateFormat)

fun isDNR(personIdent: String): Boolean
    = personIdent[0] > '3'

fun findDoctorInRelations(patient: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person, doctorPersonnumber: String): Familierelasjon? =
        patient.harFraRolleI.find {
            val aktoer = it.tilPerson.aktoer
            aktoer is PersonIdent && aktoer.ident.type.value == "FNR" && aktoer.ident.ident == doctorPersonnumber
        }

fun extractLegeerklaering(fellesformat: EIFellesformat): Legeerklaring =
        fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

fun extractSignatureDate(fellesformat: EIFellesformat): LocalDateTime =
        fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
