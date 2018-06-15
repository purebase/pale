package no.nav.pale.mapping

import no.nav.pale.model.Behandlingsvedlegg
import no.nav.pale.model.BehandlingsvedleggSender
import no.nav.pale.model.Merknad
import no.nav.pale.validation.*
import no.nav.model.fellesformat.EIFellesformat
import java.time.ZonedDateTime

fun mapFellesformatToBehandlingsVedlegg(fellesformat: EIFellesformat, outcomes: List<Outcome>): Behandlingsvedlegg {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val org = fellesformat.msgHead.msgInfo.sender.organisation
    val hcp = org.healthcareProfessional
    val orgIdent = org.ident[0]
    return Behandlingsvedlegg(
            type = "LEGEERKL",
            status = "OPPFØLGING",
            pasientId = extractPersonIdent(legeerklaering),
            ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId,
            msgId = fellesformat.msgHead.msgInfo.msgId,
            generertDato = fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime(),
            motattNavDato = fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime(),
            registrertAutomatiskBehandlingDato = ZonedDateTime.now(),
            sender = BehandlingsvedleggSender(
                    signaturId = fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur,
                    signaturNavn = fellesformat.mottakenhetBlokk.avsender,
                    avsenderId = extractDoctorIdentFromSender(fellesformat)?.id,
                    avsenderNavn = if (hcp.middleName == null) {
                        "${hcp.familyName.toUpperCase()} ${hcp.givenName.toUpperCase()}"
                    } else {
                        "${hcp.familyName.toUpperCase()} ${hcp.givenName.toUpperCase()} ${hcp.middleName.toUpperCase()}"
                    },
                    tlfNummer = hcp.teleCom.find { it.typeTelecom in PhoneType }?.teleAddress?.v,
                    organisasjonsId = "${orgIdent.id} (${orgIdent.typeId.v}",
                    organisasjonsNavn = org.organisationName,
                    adresse = org.address.streetAdr,
                    poststed = org.address.city,
                    postnummer = org.address.postalCode,
                    merknadAvvist = outcomes.filter { it.outcomeType.messagePriority == Priority.RETUR }.toMerknader(),
                    merknadManuellBehandling = outcomes.filter { it.outcomeType.messagePriority == Priority.MANUAL_PROCESSING }.toMerknader(),
                    merknadOppfoelging = outcomes.filter { it.outcomeType.messagePriority == Priority.FOLLOW_UP }.toMerknader(),
                    merknadNotis = outcomes.filter { it.outcomeType.messagePriority == Priority.NOTE }.toMerknader()
            )
    )
}

fun List<Outcome>.toMerknader():List<Merknad> =
        this.map { Merknad(it.formattedMessage, it.outcomeType.messageNumber) }
