package no.nav.legeerklaering.validation

import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

data class RuleExecutionInfo(
        val fellesformat: EIFellesformat,
        val legeerklaering: Legeerklaring,
        val patientPersonNumber: String?,
        val doctorPersonNumber: String?,
        val outcome: MutableList<Outcome>
)

fun initFlow(fellesformat: EIFellesformat): Observable<RuleExecutionInfo> =
        listOf(fellesformat).toObservable()
                .map {
                    val legeerklaering = extractLegeerklaering(it)
                    RuleExecutionInfo(
                            fellesformat = it,
                            legeerklaering = legeerklaering,
                            patientPersonNumber = extractPersonNumber(legeerklaering),
                            doctorPersonNumber = extractDoctorPersonNumberFromSender(it),
                            outcome = mutableListOf()
                    )
                }

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

fun extractBornDate(personNumber: String): LocalDate =
        LocalDate.parse(personNumber.substring(0, 6).let {
            if (it[0] > '3') {
                (it[0] - 3) + it.substring(1)
            } else {
                it
            }
        }, personNumberDateFormat)

fun findDoctorInRelations(patient: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person, doctorPersonnumber: String): Familierelasjon? =
        patient.harFraRolleI.find {
            val aktoer = it.tilPerson.aktoer
            aktoer is PersonIdent && aktoer.ident.type.value == "FNR" && aktoer.ident.ident == doctorPersonnumber
        }

fun extractLegeerklaering(fellesformat: EIFellesformat): Legeerklaring =
        fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

fun extractSignatureDate(fellesformat: EIFellesformat): LocalDateTime =
        fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
