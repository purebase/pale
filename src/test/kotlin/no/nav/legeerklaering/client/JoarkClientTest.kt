package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.getHCPFodselsnummer
import no.nav.legeerklaering.readToFellesformat
import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Assert
import org.junit.Test
import java.util.*
import javax.xml.datatype.DatatypeFactory

class JoarkClientTest {

    val fellesformat = readToFellesformat("/legeerklaering.xml")
    val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring
    val request = JoarkClient().archiveMessage(legeerklaring, fellesformat)
    val expectedCurrentDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar())


    @Test
    fun shouldSetbegrensetPartsinnsynFraTredjePartOnFagmelding() {

        Assert.assertEquals(null,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.begrensetPartsinnsynFraTredjePart)
    }

    @Test
    fun shouldSetFilnavnToEdiloggidFagmelding() {

        Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId+".pdf",
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].filnavn)
    }

    @Test
    fun shouldSetFilnavnToEdiloggidBehandlingsvedlegg() {

          Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId+"-behandlingsvedlegg.pdf",
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].filnavn)
    }

    @Test
    fun shouldSetfiltypeKodeOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.pdf.string,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].filtypeKode)
    }

    @Test
    fun shouldSetfiltypeKodeOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.pdf.string,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].filtypeKode)
    }

    @Test
    fun shouldSetvariantFormatKodeOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.arkiv.string,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].variantFormatKode)
    }

    @Test
    fun shouldSetvariantFormatKodeOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.arkiv.string,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].variantFormatKode)
    }

    @Test
    fun shouldSetfildetaljerListeversjonOnFagmelding() {

        Assert.assertEquals(1,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].versjon)
    }

    @Test
    fun shouldSetfildetaljerListeversjonOnBehandlingsvedlegg() {

        Assert.assertEquals(1,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].versjon)
    }

    @Test
    fun shouldSetkategoriKodeBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.kategoriKodeES.string,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.kategoriKode)
    }

    @Test
    fun shouldSetkategoriKodeFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.kategoriKodeES.string,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.kategoriKode)
    }

    @Test
    fun shouldSettittelOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.behandlingsVeddleggTittel.string,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.tittel)
    }

    @Test
    fun shouldSettittelOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.legeerklæring.string,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.tittel)
    }

    @Test
    fun shouldSebrevkodeOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.brevkode900002.string,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.brevkode)
    }

    @Test
    fun shouldSetbrevkodeOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.brevkode900002.string,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.brevkode)
    }

    @Test
    fun shouldSetsensitivtOnBehandlingsvedlegg() {

        Assert.assertEquals(false,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.sensitivt)
    }

    @Test
    fun shouldSetsensitivtOnFagmelding() {

        Assert.assertEquals(false,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.sensitivt)
    }

    @Test
    fun shouldSetorganInterntOnBehandlingsvedlegg() {

        Assert.assertEquals(true,
                request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.organInternt)
    }

    @Test
    fun shouldSetorganInterntOnFagmelding() {

        Assert.assertEquals(false,
                request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.organInternt)
    }

    @Test
    fun shouldSetjournalpostDokumentInfoRelasjonListeVersjonBehandlingsvedlegg() {

        Assert.assertEquals(1, request.journalpostDokumentInfoRelasjonListe[0].versjon)
    }

    @Test
    fun shouldSetjournalpostDokumentInfoRelasjonListeVersjonOnFagmelding() {

        Assert.assertEquals(1, request.journalpostDokumentInfoRelasjonListe[0].versjon)
    }

    @Test
    fun shouldSetbrukerId() {

        Assert.assertEquals(fellesformat.msgHead.msgInfo.patient.ident[0].id, request.gjelderListe[0].brukerId)
    }

    @Test
    fun shouldSetbrukertypeKode() {

        Assert.assertEquals(LegeerklaeringConstant.person.string, request.gjelderListe[0].brukertypeKode)
    }

    @Test
    fun shouldSetmerknad() {

        Assert.assertEquals(LegeerklaeringConstant.legeerklæring.string, request.merknad)
    }

    @Test
    fun shouldSetmottakskanalKode() {

        Assert.assertEquals(LegeerklaeringConstant.eia.string, request.mottakskanalKode)
    }

    @Test
    fun shouldSetmottattDatoToCurrentDate() {
        Assert.assertEquals(expectedCurrentDate.day, request.mottattDato.day)
        Assert.assertEquals(expectedCurrentDate.hour, request.mottattDato.hour)
        Assert.assertEquals(expectedCurrentDate.minute, request.mottattDato.minute)
    }

    @Test
    fun shouldSetinnhold() {

        Assert.assertEquals(LegeerklaeringConstant.legeerklæring.string, request.innhold)
    }

    @Test
    fun shouldSetjournalForendeEnhetIdToNull() {

        Assert.assertEquals(null, request.journalForendeEnhetId)
    }

    @Test
    fun shouldSetjournalposttypeKode() {

        Assert.assertEquals(LegeerklaeringConstant.journalposttypeKodeI.string, request.journalposttypeKode)
    }

    @Test
    fun shouldSetjournalstatusKode() {

        Assert.assertEquals(LegeerklaeringConstant.journalstatusKodeMO.string, request.journalstatusKode)
    }

    @Test
    fun shouldSetdokumentDatoToCurrentDate() {

        Assert.assertEquals(expectedCurrentDate.day, request.dokumentDato.day)
        Assert.assertEquals(expectedCurrentDate.hour, request.dokumentDato.hour)
        Assert.assertEquals(expectedCurrentDate.minute, request.dokumentDato.minute)
    }

    @Test
    fun shouldSetfagomradeKode() {

        Assert.assertEquals(LegeerklaeringConstant.opp.string, request.fagomradeKode)
    }

    @Test
    fun shouldSetfordeling() {

        Assert.assertEquals(LegeerklaeringConstant.eiaOk.string, request.fordeling)
    }

    @Test
    fun shouldSetavsenderMottaker() {

        Assert.assertEquals(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName.toUpperCase()+ " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName.toUpperCase() + " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName.toUpperCase(), request.avsenderMottaker)
    }

    @Test
    fun shouldSetavsenderMottakerId() {

        Assert.assertEquals(getHCPFodselsnummer(fellesformat), request.avsenderMottakerId)
    }

    @Test
    fun shouldSetopprettetAvNavn() {

        Assert.assertEquals(LegeerklaeringConstant.eiaAuto.string, request.opprettetAvNavn)
    }
}
