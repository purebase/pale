package no.nav.pale.client

import no.nav.pale.PaleConstant
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.getHCPFodselsnummer
import no.nav.pale.mapping.formatName
import no.nav.pale.validation.extractLegeerklaering
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

object JoarkClientSpek : Spek({
    val bytes: ByteArray = listOf(0xDE, 0xAD, 0xBE, 0xEF).map { it.toByte() }.toByteArray()
    val currentDate = LocalDateTime.now()
    describe("Normal request") {
        val fellesformat = defaultFellesformat(defaultPerson())
        val ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
        val request = createJoarkRequest(fellesformat, bytes, bytes, false)
        it("Sets restricted for third party") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.begrensetPartsinnsynFraTredjePart shouldEqual
                    (extractLegeerklaering(fellesformat).forbeholdLegeerklaring.tilbakeholdInnhold.toInt() != 2)
        }
        it("Sets file name to $ediLoggId.pdf on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].filnavn shouldEqual
                    "$ediLoggId.pdf"
        }
        it("Sets pdf as filetype on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].filtypeKode shouldEqual
                    PaleConstant.pdf.string
        }
        it("Sets formatkode on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].variantFormatKode shouldEqual
                    PaleConstant.arkiv.string
        }
        it("Sets file name to $ediLoggId-behandlingsvedlegg.pdf on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].filnavn shouldEqual
                    "$ediLoggId-behandlingsvedlegg.pdf"
        }
        it("Sets pdf as filetype on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].filtypeKode shouldEqual
                    PaleConstant.pdf.string
        }
        it("Sets arkiv as variant format code on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].variantFormatKode shouldEqual
                    PaleConstant.arkiv.string
        }
        it("Sets file detail list version on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.fildetaljerListe[0].versjon shouldEqual 1
        }
        it("Sets file detail list version on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.fildetaljerListe[0].versjon shouldEqual 1
        }
        it("Sets category code behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.kategoriKode shouldEqual
                    PaleConstant.kategoriKodeES.string
        }
        it("Sets category code in fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.kategoriKode shouldEqual
                    PaleConstant.kategoriKodeES.string
        }
        it("Sets the document title on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.tittel shouldEqual
                    PaleConstant.behandlingsVeddleggTittel.string
        }
        it("Sets the document title on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.tittel shouldEqual
                    PaleConstant.legeerklæring.string
        }
        it("Sets letter code on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.brevkode shouldEqual
                    PaleConstant.brevkode900002.string
        }
        it("Sets letter code on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.brevkode shouldEqual
                    PaleConstant.brevkode900002.string
        }
        it("Sets sensitivity on document info for behandlingsinfo") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.sensitivt shouldEqual false
        }
        it("Sets sensitivity on document info for fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.sensitivt shouldEqual false
        }
        it("Sets organization internal to true on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[1].dokumentInfo.organInternt shouldEqual true
        }
        it("Sets organization internal to false on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[0].dokumentInfo.organInternt shouldEqual false
        }
        it("Sets relation list version on behandlingsvedlegg") {
            request.journalpostDokumentInfoRelasjonListe[0].versjon shouldEqual 1
        }
        it("Sets relation list version on fagmelding") {
            request.journalpostDokumentInfoRelasjonListe[1].versjon shouldEqual 1
        }
        it("Sets user id on applies to list") {
            request.gjelderListe[0].brukerId shouldEqual fellesformat.msgHead.msgInfo.patient.ident[0].id
        }
        it("Sets user type code") {
            request.gjelderListe[0].brukertypeKode shouldEqual PaleConstant.person.string
        }
        it("Sets mottakskode") {
            request.mottakskanalKode shouldEqual PaleConstant.eia.string
        }
        it("Sets received date to current date") {
            request.mottattDato.month shouldEqual currentDate.monthValue
            request.mottattDato.day shouldEqual currentDate.dayOfMonth
            request.mottattDato.hour shouldEqual currentDate.hour
        }
        it("Sets content") {
            request.innhold shouldEqual "Legeerklæring"
        }
        it("Sets feeding unit to null") {
            request.journalForendeEnhetId.shouldBeNull()
        }
        it("Sets journal post type code") {
            request.journalposttypeKode shouldEqual PaleConstant.journalposttypeKodeI.string
        }
        it("Sets journal status code") {
            request.journalstatusKode shouldEqual PaleConstant.journalstatusKodeMO.string
        }
        it("Sets document date to current date") {
            request.dokumentDato.month shouldEqual currentDate.monthValue
            request.dokumentDato.day shouldEqual currentDate.dayOfMonth
            request.dokumentDato.hour shouldEqual currentDate.hour
        }
        it("Sets fagområde code") {
            request.fagomradeKode shouldEqual PaleConstant.opp.string
        }
        it("Should set distribution") {
            request.fordeling shouldEqual PaleConstant.eiaOk.string
        }
        it("Sets sender receiver") {
            request.avsenderMottaker shouldEqual
                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.formatName()
        }
        it("Sets sender received id") {
            request.avsenderMottakerId shouldEqual getHCPFodselsnummer(fellesformat)
        }
        it("Sets opprettet av") {
            request.opprettetAvNavn shouldEqual PaleConstant.eiaAuto.string
        }
        it("Sets merknad") {
            request.merknad shouldEqual PaleConstant.legeerklæring.string
        }
    }
    describe("Fellesformat without hcp") {
        // TODO: Should this really result in a empty field rather then denying the message?
        val fellesformat = defaultFellesformat(defaultPerson())
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional = null
        val request = createJoarkRequest(fellesformat, bytes, bytes, false)
        it("Sets empty sender receiver if HCP is missing") {
            request.avsenderMottaker shouldEqual ""
        }
    }
})
