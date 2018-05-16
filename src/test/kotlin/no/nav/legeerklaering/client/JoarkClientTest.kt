package no.nav.legeerklaering.client

import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.getHCPFodselsnummer
import no.nav.legeerklaering.readToFellesformat
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.legeerklaering.validation.toOutcome
import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Assert
import org.junit.Test
import java.util.*
import javax.xml.datatype.DatatypeFactory

class JoarkClientTest {

    val fellesformat = readToFellesformat("/legeerklaering.xml")
    val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring
    val requestZeroOutcomes = createJoarkRequest(legeerklaring, fellesformat, null)
    val outcomes = listOf(OutcomeType.PERSON_NUMBER_NOT_FOUND.toOutcome())
    val requestWithOutcomes = createJoarkRequest(legeerklaring, fellesformat, outcomes)
    val expectedCurrentDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar())


    @Test
    fun shouldSetbegrensetPartsinnsynFraTredjePartOnFagmelding() {

        Assert.assertEquals(null,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.begrensetPartsinnsynFraTredjePart)
    }

    @Test
    fun shouldSetFilnavnToEdiloggidFagmelding() {

        Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId+".pdf",
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].filnavn)
    }

    @Test
    fun shouldSetFilnavnToEdiloggidBehandlingsvedlegg() {

          Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId+"-behandlingsvedlegg.pdf",
                  requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].filnavn)
    }

    @Test
    fun shouldSetfiltypeKodeOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.pdf.string,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].filtypeKode)
    }

    @Test
    fun shouldSetfiltypeKodeOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.pdf.string,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].filtypeKode)
    }

    @Test
    fun shouldSetvariantFormatKodeOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.arkiv.string,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].variantFormatKode)
    }

    @Test
    fun shouldSetvariantFormatKodeOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.arkiv.string,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].variantFormatKode)
    }

    @Test
    fun shouldSetfildetaljerListeversjonOnFagmelding() {

        Assert.assertEquals(1,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].versjon)
    }

    @Test
    fun shouldSetfildetaljerListeversjonOnBehandlingsvedlegg() {

        Assert.assertEquals(1,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].versjon)
    }

    @Test
    fun shouldSetkategoriKodeBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.kategoriKodeES.string,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.kategoriKode)
    }

    @Test
    fun shouldSetkategoriKodeFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.kategoriKodeES.string,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.kategoriKode)
    }

    @Test
    fun shouldSettittelOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.behandlingsVeddleggTittel.string,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.tittel)
    }

    @Test
    fun shouldSettittelOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.legeerklæring.string,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.tittel)
    }

    @Test
    fun shouldSebrevkodeOnBehandlingsvedlegg() {

        Assert.assertEquals(LegeerklaeringConstant.brevkode900002.string,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.brevkode)
    }

    @Test
    fun shouldSetbrevkodeOnFagmelding() {

        Assert.assertEquals(LegeerklaeringConstant.brevkode900002.string,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.brevkode)
    }

    @Test
    fun shouldSetsensitivtOnBehandlingsvedlegg() {

        Assert.assertEquals(false,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.sensitivt)
    }

    @Test
    fun shouldSetsensitivtOnFagmelding() {

        Assert.assertEquals(false,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.sensitivt)
    }

    @Test
    fun shouldSetorganInterntOnBehandlingsvedlegg() {

        Assert.assertEquals(true,
                requestWithOutcomes.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.organInternt)
    }

    @Test
    fun shouldSetorganInterntOnFagmelding() {

        Assert.assertEquals(false,
                requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.organInternt)
    }

    @Test
    fun shouldSetjournalpostDokumentInfoRelasjonListeVersjonBehandlingsvedlegg() {

        Assert.assertEquals(1, requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].versjon)
    }

    @Test
    fun shouldSetjournalpostDokumentInfoRelasjonListeVersjonOnFagmelding() {

        Assert.assertEquals(1, requestZeroOutcomes.journalpostDokumentInfoRelasjonListe[0].versjon)
    }

    @Test
    fun shouldSetbrukerId() {

        Assert.assertEquals(fellesformat.msgHead.msgInfo.patient.ident[0].id, requestZeroOutcomes.gjelderListe[0].brukerId)
    }

    @Test
    fun shouldSetbrukertypeKode() {

        Assert.assertEquals(LegeerklaeringConstant.person.string, requestZeroOutcomes.gjelderListe[0].brukertypeKode)
    }

    @Test
    fun shouldSetmerknad() {

        Assert.assertEquals(LegeerklaeringConstant.legeerklæring.string, requestZeroOutcomes.merknad)
    }

    @Test
    fun shouldSetmottakskanalKode() {

        Assert.assertEquals(LegeerklaeringConstant.eia.string, requestZeroOutcomes.mottakskanalKode)
    }

    @Test
    fun shouldSetmottattDatoToCurrentDate() {
        Assert.assertEquals(expectedCurrentDate.day, requestZeroOutcomes.mottattDato.day)
        Assert.assertEquals(expectedCurrentDate.hour, requestZeroOutcomes.mottattDato.hour)
        Assert.assertEquals(expectedCurrentDate.minute, requestZeroOutcomes.mottattDato.minute)
    }

    @Test
    fun shouldSetinnhold() {

        Assert.assertEquals(LegeerklaeringConstant.legeerklæring.string, requestZeroOutcomes.innhold)
    }

    @Test
    fun shouldSetjournalForendeEnhetIdToNull() {

        Assert.assertEquals(null, requestZeroOutcomes.journalForendeEnhetId)
    }

    @Test
    fun shouldSetjournalposttypeKode() {

        Assert.assertEquals(LegeerklaeringConstant.journalposttypeKodeI.string, requestZeroOutcomes.journalposttypeKode)
    }

    @Test
    fun shouldSetjournalstatusKode() {

        Assert.assertEquals(LegeerklaeringConstant.journalstatusKodeMO.string, requestZeroOutcomes.journalstatusKode)
    }

    @Test
    fun shouldSetdokumentDatoToCurrentDate() {

        Assert.assertEquals(expectedCurrentDate.day, requestZeroOutcomes.dokumentDato.day)
        Assert.assertEquals(expectedCurrentDate.hour, requestZeroOutcomes.dokumentDato.hour)
        Assert.assertEquals(expectedCurrentDate.minute, requestZeroOutcomes.dokumentDato.minute)
    }

    @Test
    fun shouldSetfagomradeKode() {

        Assert.assertEquals(LegeerklaeringConstant.opp.string, requestZeroOutcomes.fagomradeKode)
    }

    @Test
    fun shouldSetfordeling() {

        Assert.assertEquals(LegeerklaeringConstant.eiaOk.string, requestZeroOutcomes.fordeling)
    }

    @Test
    fun shouldSetavsenderMottaker() {

        Assert.assertEquals(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName.toUpperCase()+ " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName.toUpperCase() + " " +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName.toUpperCase(), requestZeroOutcomes.avsenderMottaker)
    }

    @Test
    fun shouldSetavsenderMottakerId() {

        Assert.assertEquals(getHCPFodselsnummer(fellesformat), requestZeroOutcomes.avsenderMottakerId)
    }

    @Test
    fun shouldSetopprettetAvNavn() {

        Assert.assertEquals(LegeerklaeringConstant.eiaAuto.string, requestZeroOutcomes.opprettetAvNavn)
    }
}
