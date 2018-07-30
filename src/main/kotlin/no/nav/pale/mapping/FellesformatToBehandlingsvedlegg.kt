package no.nav.pale.mapping

import no.nav.pale.model.Behandlingsvedlegg
import no.nav.pale.model.BehandlingsvedleggSender
import no.nav.pale.model.Merknad
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.msghead.HealthcareProfessional
import no.nav.pale.PaleConstant
import no.nav.pale.validation.Outcome
import no.nav.pale.validation.Priority
import no.nav.pale.validation.extractDoctorIdentFromSignature
import no.nav.pale.validation.extractLegeerklaering
import no.nav.pale.validation.extractPersonIdent
import java.time.ZonedDateTime

fun mapFellesformatToBehandlingsVedlegg(fellesformat: EIFellesformat, outcomes: List<Outcome>): Behandlingsvedlegg {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val org = fellesformat.msgHead.msgInfo.sender.organisation
    val hcp = org.healthcareProfessional
    val orgIdent = org.ident[0]
    return Behandlingsvedlegg(
            type = fellesformat.msgHead.msgInfo.type.v,
            status = PaleConstant.OPPFØLGING.string,
            pasientId = extractPersonIdent(legeerklaering),
            ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId,
            msgId = fellesformat.msgHead.msgInfo.msgId,
            generertDato = fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime(),
            motattNavDato = fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime(),
            registrertAutomatiskBehandlingDato = ZonedDateTime.now(),
            sender = BehandlingsvedleggSender(
                    signaturId = fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur,
                    signaturNavn = fellesformat.mottakenhetBlokk.avsender,
                    avsenderId = extractDoctorIdentFromSignature(fellesformat),
                    avsenderNavn = hcp?.formatName() ?: "",
                    tlfNummer = hcp?.teleCom?.find { it.typeTelecom in PhoneType }?.teleAddress?.v,
                    organisasjonsId = "${orgIdent.id} (${orgIdent.typeId.v}",
                    organisasjonsNavn = org.organisationName,
                    adresse = org.address?.streetAdr,
                    poststed = org.address?.city,
                    postnummer = org.address?.postalCode,
                    merknadAvvist = outcomes.filter { it.outcomeType.messagePriority == Priority.RETUR }.toMerknader(),
                    merknadManuellBehandling = outcomes.filter { it.outcomeType.messagePriority == Priority.MANUAL_PROCESSING }.toMerknader(),
                    merknadOppfoelging = outcomes.filter { it.outcomeType.messagePriority == Priority.FOLLOW_UP }.toMerknader(),
                    merknadNotis = outcomes.filter { it.outcomeType.messagePriority == Priority.NOTE }.toMerknader()
            )
    )
}

fun HealthcareProfessional.formatName(): String = if (middleName == null) {
    "${familyName.toUpperCase()} ${givenName.toUpperCase()}"
} else {
    "${familyName.toUpperCase()} ${givenName.toUpperCase()} ${middleName.toUpperCase()}"
}

fun List<Outcome>.toMerknader(): List<Merknad> =
        this.map { Merknad(it.formattedMessage, it.outcomeType.messageNumber) }
