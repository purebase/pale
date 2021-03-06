package no.nav.pale.mapping

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.generatePersonNumber
import no.nav.pale.objectMapper
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.toOutcome
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object FellesformatToBehandlingsvedleggSpek : Spek({
    describe("Result with outcomes") {
        val outcomes = listOf(
                OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND.toOutcome(
                        generatePersonNumber(LocalDate.now().minusYears(40)),
                        apprecError = ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA),
                OutcomeType.CHILD_OF_PATIENT.toOutcome(),
                OutcomeType.BEHANDLER_HAS_FNR_USES_DNR.toOutcome(),
                OutcomeType.PATIENT_EMIGRATED.toOutcome())
        val fellesformat = defaultFellesformat(defaultPerson())
        it("Won't cause an exception") {
            println(objectMapper.writeValueAsString(mapFellesformatToBehandlingsVedlegg(fellesformat, outcomes)))
        }
    }

    describe("Health care professional with middle name") {
        val fellesformat = defaultFellesformat(defaultPerson(),
                defaultPerson().withPersonnavn(Personnavn()
                        .withFornavn("Fornavn")
                        .withMellomnavn("Mellomnavn")
                        .withEtternavn("Etternavnsen"))
        )
        it("Creates expected formatted name") {
            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.formatName() shouldEqual
                    "ETTERNAVNSEN FORNAVN MELLOMNAVN"
        }
    }

    describe("Health care professional with no middle name") {
        val fellesformat = defaultFellesformat(defaultPerson(),
                defaultPerson().withPersonnavn(Personnavn()
                        .withFornavn("Fornavn")
                        .withEtternavn("Etternavnsen"))
        )
        it("Creates expected formatted name") {
            fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.formatName() shouldEqual
                    "ETTERNAVNSEN FORNAVN"
        }
    }
})
