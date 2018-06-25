package no.nav.pale.client

import no.nav.pale.PaleConstant
import no.nav.pale.metrics.MESSAGE_OUTCOME_COUNTER
import no.nav.pale.newInstance
import no.nav.pale.validation.extractLegeerklaering
import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.mapping.formatName
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.Bruker
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.DokumentInfo
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.Fildetaljer
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.JournalpostDokumentInfoRelasjon
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.LagreDokumentOgOpprettJournalpostRequest
import java.util.GregorianCalendar

fun createJoarkRequest(fellesformat: EIFellesformat, fagmelding: ByteArray, behandlingsvedlegg: ByteArray?, manuelBehandling: Boolean):
        LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {
    val hcp = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional
    val legeerklaering = extractLegeerklaering(fellesformat)

    journalpostDokumentInfoRelasjonListe.add(mapfellesformatToDokumentInfoRelasjon(fellesformat, false, fagmelding))

    if (behandlingsvedlegg != null) {
        journalpostDokumentInfoRelasjonListe.add(mapfellesformatToDokumentInfoRelasjon(fellesformat, true, behandlingsvedlegg))
    }

    gjelderListe.add(Bruker().apply {
        brukerId = legeerklaering.pasientopplysninger.pasient.fodselsnummer
        brukertypeKode = PaleConstant.person.string
    })

    merknad = PaleConstant.legeerklæring.string
    mottakskanalKode = PaleConstant.eia.string
    mottattDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
    innhold = PaleConstant.legeerklæring.string
    journalForendeEnhetId = null
    journalposttypeKode = PaleConstant.journalposttypeKodeI.string
    journalstatusKode = PaleConstant.journalstatusKodeMO.string
    dokumentDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
    fagomradeKode = PaleConstant.opp.string
    fordeling = when(manuelBehandling){
              true -> PaleConstant.eiaMan.string
              false -> PaleConstant.eiaOk.string
    }
    avsenderMottaker = hcp.formatName()
    avsenderMottakerId = fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur
    opprettetAvNavn = PaleConstant.eiaAuto.string
}

fun mapfellesformatToDokumentInfoRelasjon(fellesformat: EIFellesformat, behandlingsvedlegg: Boolean, pdfDocumentBase64: ByteArray):
        JournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
    dokumentInfo = DokumentInfo().apply {
        begrensetPartsinnsynFraTredjePart = extractLegeerklaering(fellesformat).forbeholdLegeerklaring?.tilbakeholdInnhold?.toInt() != 2

        fildetaljerListe.add(Fildetaljer().apply {
            fil = pdfDocumentBase64
            filnavn = when (behandlingsvedlegg) {
                true -> "${fellesformat.mottakenhetBlokk.ediLoggId}-behandlingsvedlegg${PaleConstant.pdfType.string}"
                false -> "${fellesformat.mottakenhetBlokk.ediLoggId}${PaleConstant.pdfType.string}"
            }
            filtypeKode = PaleConstant.pdf.string
            variantFormatKode = PaleConstant.arkiv.string
            versjon = 1
        })
        kategoriKode = PaleConstant.kategoriKodeES.string

        tittel = when (behandlingsvedlegg) {
            true -> PaleConstant.behandlingsVeddleggTittel.string
            false -> PaleConstant.legeerklæring.string
        }
        brevkode = PaleConstant.brevkode900002.string
        sensitivt = false
        organInternt = behandlingsvedlegg
        versjon = 1
    }
    tilknyttetJournalpostSomKode = when (behandlingsvedlegg) {
        true -> PaleConstant.vedlegg.string
        false ->    PaleConstant.houveddokument.string
    }
    tilknyttetAvNavn = PaleConstant.eiaAuto.string
    versjon = 1
}
