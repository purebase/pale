package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.getHCPFodselsnummer
import no.nav.legeerklaering.newInstance
import no.nav.legeerklaering.validation.extractLegeerklaering
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.*
import java.util.*


fun createJoarkRequest(fellesformat: EIFellesformat, legeerklaering: Legeerklaring, fagmelding: ByteArray, behandlingsvedlegg: ByteArray?, manuelBehandling: Boolean):
        LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {
    val hcp = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional

    journalpostDokumentInfoRelasjonListe.add(mapfellesformatToDokumentInfoRelasjon(fellesformat, false, fagmelding))

    if(behandlingsvedlegg != null) {
        journalpostDokumentInfoRelasjonListe.add(mapfellesformatToDokumentInfoRelasjon(fellesformat, true, behandlingsvedlegg))
    }

    gjelderListe.add(Bruker().apply {
        brukerId = legeerklaering.pasientopplysninger.pasient.fodselsnummer
        brukertypeKode = LegeerklaeringConstant.person.string
    })

    merknad = LegeerklaeringConstant.legeerklæring.string
    mottakskanalKode = LegeerklaeringConstant.eia.string
    mottattDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
    innhold = LegeerklaeringConstant.legeerklæring.string
    journalForendeEnhetId = null
    journalposttypeKode =  LegeerklaeringConstant.journalposttypeKodeI.string
    journalstatusKode = LegeerklaeringConstant.journalstatusKodeMO.string
    dokumentDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
    fagomradeKode = LegeerklaeringConstant.opp.string

    if (manuelBehandling){
        fordeling = LegeerklaeringConstant.eiaMan.string
    }
    fordeling = LegeerklaeringConstant.eiaOk.string
    avsenderMottaker = "${hcp.familyName.toUpperCase()} ${hcp.givenName.toUpperCase()} ${hcp.middleName.toUpperCase()}"
    avsenderMottakerId = fellesformat.mottakenhetBlokk.avsenderFnrFraDigSignatur
    opprettetAvNavn = LegeerklaeringConstant.eiaAuto.string
}


fun mapfellesformatToDokumentInfoRelasjon(fellesformat: EIFellesformat, behandlingsvedlegg: Boolean, pdfDocumentBase64: ByteArray):
        JournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
    dokumentInfo = DokumentInfo().apply {
        begrensetPartsinnsynFraTredjePart = extractLegeerklaering(fellesformat).forbeholdLegeerklaring.tilbakeholdInnhold.toInt() != 2

        fildetaljerListe.add(Fildetaljer().apply {
            fil = pdfDocumentBase64
            filnavn = when (behandlingsvedlegg) {
                true -> "${fellesformat.mottakenhetBlokk.ediLoggId}-behandlingsvedlegg${LegeerklaeringConstant.pdfType.string}"
                false -> "${fellesformat.mottakenhetBlokk.ediLoggId}${LegeerklaeringConstant.pdfType.string}"
            }
            filtypeKode = LegeerklaeringConstant.pdf.string
            variantFormatKode = LegeerklaeringConstant.arkiv.string
            versjon = 1
        })
        kategoriKode = LegeerklaeringConstant.kategoriKodeES.string

        tittel = when (behandlingsvedlegg) {
            true -> LegeerklaeringConstant.behandlingsVeddleggTittel.string
            false -> LegeerklaeringConstant.legeerklæring.string
        }
        brevkode = LegeerklaeringConstant.brevkode900002.string
        sensitivt = false
        organInternt = behandlingsvedlegg
        versjon = 1
    }
    if(behandlingsvedlegg) {
        tilknyttetJournalpostSomKode = LegeerklaeringConstant.vedlegg.string
    }
    tilknyttetJournalpostSomKode =  LegeerklaeringConstant.houveddokument.string
    tilknyttetAvNavn =  LegeerklaeringConstant.eiaAuto.string
    versjon = 1
}
