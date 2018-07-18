package no.nav.pale.datagen

import no.nav.pale.validation.OutcomeType
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files
import java.nio.file.Paths

class OutcomeTypeCsvGeneratorSpek : Spek({
    describe("OutcomeType") {
        it("Generates a OutcomeType CSV") {
            val header = "Referanse i kode;Merknad nr;Beskrivelse;Prioritet;Type"
            val csv = OutcomeType.values()
                    .map { "${it.name};${it.messageNumber};${it.messageText};${it.messagePriority};${it.messageType}" }
            val basePath = Paths.get("build", "reports")
            Files.createDirectories(basePath)
            Files.write(basePath.resolve("rules.csv"), listOf(header) + csv, Charsets.UTF_8)
        }
    }
})
