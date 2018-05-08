package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringApplication
import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.newInstance
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.*
import java.util.*

class JoarkClient {

fun archiveMessage(legeeklaering: Legeerklaring, fellesformat: EIFellesformat):
        LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {

    val fagmeldingJournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
        dokumentInfo = DokumentInfo().apply {

            if (legeeklaering.forbeholdLegeerklaring.tilbakeholdInnhold != 2.toBigInteger()) {
                begrensetPartsinnsynFraTredjePart = true}

            fildetaljerListe.add(Fildetaljer().apply {
                //TODO fil = createPDFBase64Encoded(legeeklaering)
                filnavn = fellesformat.mottakenhetBlokk.ediLoggId + LegeerklaeringConstant.pdfType.string
                filtypeKode = LegeerklaeringConstant.pdf.string
                variantFormatKode = LegeerklaeringConstant.arkiv.string
                versjon = 1
            })
            kategoriKode = LegeerklaeringConstant.kategoriKodeES.string
            tittel = LegeerklaeringConstant.legeerklæring.string
            brevkode = LegeerklaeringConstant.brevkode900002.string
            sensitivt = false
            organInternt = false
            versjon = 1
        }
        tilknyttetJournalpostSomKode = "HOVEDDOKUMENT"
        tilknyttetAvNavn =  LegeerklaeringConstant.eiaAuto.string
        versjon = 1
    }

    val behandlingsvedleggJournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
        dokumentInfo = DokumentInfo().apply {

            fildetaljerListe.add(Fildetaljer().apply {
                //TODO = createPDFBase64Encoded(legeeklaering)
                filnavn = fellesformat.mottakenhetBlokk.ediLoggId+"-behandlingsvedlegg"+
                        LegeerklaeringConstant.pdfType.string
                filtypeKode = LegeerklaeringConstant.pdf.string
                variantFormatKode = LegeerklaeringConstant.arkiv.string
                versjon = 1
            })
            kategoriKode = LegeerklaeringConstant.kategoriKodeES.string
            tittel = LegeerklaeringConstant.behandlingsVeddleggTittel.string
            brevkode = LegeerklaeringConstant.brevkode900002.string
            sensitivt = false
            organInternt = true
            versjon = 1
        }
        tilknyttetJournalpostSomKode = LegeerklaeringConstant.vedlegg.string
        tilknyttetAvNavn =  LegeerklaeringConstant.eiaAuto.string
        versjon = 1
    }

    journalpostDokumentInfoRelasjonListe.add(fagmeldingJournalpostDokumentInfoRelasjon)
    journalpostDokumentInfoRelasjonListe.add(behandlingsvedleggJournalpostDokumentInfoRelasjon)

    gjelderListe.add(Bruker().apply {
        brukerId = fellesformat.msgHead.msgInfo.patient.ident[0].id
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
    fordeling = LegeerklaeringConstant.eiaOk.string
    avsenderMottaker = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName.toUpperCase()+ " " +
            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName.toUpperCase() + " " +
            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName.toUpperCase()
    avsenderMottakerId = LegeerklaeringApplication().getHCPFodselsnummer(fellesformat)
    opprettetAvNavn = LegeerklaeringConstant.eiaAuto.string
    }
}