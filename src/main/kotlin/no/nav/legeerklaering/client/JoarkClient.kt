package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.getHCPFodselsnummer
import no.nav.legeerklaering.newInstance
import no.nav.legeerklaering.validation.Outcome
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.*
import java.util.*


fun archiveMessage(legeeklaering: Legeerklaring, fellesformat: EIFellesformat, outcomes: List<Outcome>?):
        LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {

    val fagmeldingJournalpostDokumentInfoRelasjon = mapfellesformatToJournalpostDokumentInfoRelasjon(legeeklaering,fellesformat, false)
    journalpostDokumentInfoRelasjonListe.add(fagmeldingJournalpostDokumentInfoRelasjon)
    if(outcomes != null) {
        val behandlingsvedleggJournalpostDokumentInfoRelasjon = mapfellesformatToJournalpostDokumentInfoRelasjon(legeeklaering, fellesformat, true)
        journalpostDokumentInfoRelasjonListe.add(behandlingsvedleggJournalpostDokumentInfoRelasjon)
    }

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
    avsenderMottakerId = getHCPFodselsnummer(fellesformat)
    opprettetAvNavn = LegeerklaeringConstant.eiaAuto.string
    }


    fun mapfellesformatToJournalpostDokumentInfoRelasjon(legeeklaering: Legeerklaring, fellesformat: EIFellesformat, behandlingsvedlegg: Boolean):
            JournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
        dokumentInfo = DokumentInfo().apply {

            if (legeeklaering.forbeholdLegeerklaring.tilbakeholdInnhold != 2.toBigInteger()) {
                begrensetPartsinnsynFraTredjePart = true}

            fildetaljerListe.add(Fildetaljer().apply {
                //TODO fil = createPDFBase64Encoded(legeeklaering)
                filnavn = if(behandlingsvedlegg) {
                    fellesformat.mottakenhetBlokk.ediLoggId+"-behandlingsvedlegg"+
                            LegeerklaeringConstant.pdfType.string
                }
                else{
                    fellesformat.mottakenhetBlokk.ediLoggId + LegeerklaeringConstant.pdfType.string
                }
                fil
                filtypeKode = LegeerklaeringConstant.pdf.string
                variantFormatKode = LegeerklaeringConstant.arkiv.string
                versjon = 1
            })
            kategoriKode = LegeerklaeringConstant.kategoriKodeES.string

            tittel = if(behandlingsvedlegg) {
                LegeerklaeringConstant.behandlingsVeddleggTittel.string
            }
            else{
                LegeerklaeringConstant.legeerklæring.string
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
