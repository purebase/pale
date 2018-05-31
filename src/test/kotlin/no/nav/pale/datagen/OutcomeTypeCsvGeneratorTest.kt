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
        Files.write(Paths.get("build/reports/rules.csv"), listOf(header) + csv, Charsets.UTF_8)
    }
}
