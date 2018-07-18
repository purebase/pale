package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.RelationType
import no.nav.pale.datagen.createFamilyRelation
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.ident
import no.nav.pale.utils.readToFellesformat
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.Month

object ValidationHelperSpek : Spek({
    describe("Validation helper tests with fnr") {
        val fellesformat: EIFellesformat = readToFellesformat("/legeerklaering.xml")
        val legeerklaring: Legeerklaring = extractLegeerklaering(fellesformat)
        it("Extracts doctor ident from sender") {
            extractDoctorIdentFromSender(fellesformat)?.id shouldEqual "04030350265"
        }
        it("Extracts doctors ident from signature") {
            extractDoctorIdentFromSignature(fellesformat) shouldEqual "04030350265"
        }
        it("Extracts sender organization name") {
            extractSenderOrganisationName(fellesformat) shouldEqual "Kule helsetjenester AS"
        }
        it("Extracts patients ident") {
            extractPersonIdent(legeerklaring) shouldEqual "12128913767"
        }
        it("Extracts patients surname") {
            extractPatientSurname(legeerklaring) shouldEqual "Bergheim"
        }
        it("Extracts persons first name") {
            extractPatientFirstName(legeerklaring) shouldEqual "Daniel"
        }
        it("Extracts signature date") {
            val signatureDate = extractSignatureDate(fellesformat)

            signatureDate.year shouldEqual 2017
            signatureDate.month shouldEqual Month.DECEMBER
            signatureDate.dayOfMonth shouldEqual 29
        }
        it("Extracts organization number from sender") {
            extractOrganisationNumberFromSender(fellesformat)?.id shouldEqual "223456789"
        }
    }
    describe("Validation helper tests with DNR") {
        val fellesformatWithDNr: EIFellesformat = readToFellesformat("/validation/legeerklaeringWithDNR.xml")
        it("Extracts doctor person number from sender") {
            extractDoctorIdentFromSender(fellesformatWithDNr)?.id shouldEqual "45069800525"
        }
    }
    describe("Pure helper method tests") {
        it("Causes 19xx as born date when induvidual number is pre 500") {
            // We're using the first possible valid person number for the date 1.1.1905
            extractBornDate("03110511220").year shouldEqual 1905
        }
        it("Extracts born date from person number") {
            extractBornDate("12128913767") shouldEqual LocalDate.of(1989, 12, 12)
        }
        it("Extracts born date with DNR") {
            extractBornDate("45069800525") shouldEqual LocalDate.of(1998, 6, 5)
        }
        it("Finds doctor in family relations") {
            val doctor = defaultPerson()
            val patient = defaultPerson(familyRelations = arrayOf(createFamilyRelation("EKTE", doctor)))
            findDoctorInRelations(patient, doctor.ident())?.tilRolle?.value shouldEqual RelationType.EKTEFELLE.kodeverkVerdi
        }
    }
})
