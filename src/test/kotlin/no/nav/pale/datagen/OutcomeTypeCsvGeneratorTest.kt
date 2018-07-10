package no.nav.pale.datagen

import no.nav.pale.validation.OutcomeType
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class OutcomeTypeCsvGeneratorTest {
    @Test
    fun generateOutcomeTypeCSV() {
        val header = "Referanse i kode;Merknad nr;Beskrivelse;Prioritet;Type"
        val csv = OutcomeType.values()
                .map { "${it.name};${it.messageNumber};${it.messageText};${it.messagePriority};${it.messageType}" }
        val basePath = Paths.get("build", "reports")
        Files.createDirectories(basePath)
        Files.write(basePath.resolve("rules.csv"), listOf(header) + csv, Charsets.UTF_8)
    }
}
