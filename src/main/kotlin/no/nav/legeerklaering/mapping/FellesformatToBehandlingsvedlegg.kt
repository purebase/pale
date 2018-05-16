package no.nav.legeerklaering.mapping

import no.nav.legeerklaering.model.Behandlingsvedlegg
import no.nav.legeerklaering.model.BehandlingsvedleggSender
import no.nav.legeerklaering.model.Merknad
import no.nav.legeerklaering.validation.*
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
            pasientId = extractPersonNumber(legeerklaering),
            ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId,
            msgId = fellesformat.msgHead.msgInfo.msgId,
            generertDato = fellesformat.msgHead.msgInfo.genDate.toGregorianCalendar().toZonedDateTime(),
            motattNavDato = fellesformat.mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime(),
            registrertAutomatiskBehandlingDato = ZonedDateTime.now(),
            sender = BehandlingsvedleggSender(
                    signaturId = fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur,
                    signaturNavn = fellesformat.mottakenhetBlokk.avsender,
                    avsenderId = extractDoctorPersonNumberFromSender(fellesformat),
                    avsenderNavn = "${hcp.familyName.toUpperCase()} ${hcp.givenName.toUpperCase()} ${hcp.middleName.toUpperCase()}",
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
