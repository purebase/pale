package no.nav.pale.utils

import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.pale.arenaEiaInfoJaxBContext
import no.nav.pale.toString
import no.nav.pale.validation.Outcome
import no.nav.pale.validation.OutcomeType
import org.junit.Assert.assertTrue
import javax.xml.bind.Marshaller

val paeim = arenaEiaInfoJaxBContext.createMarshaller().apply {
    setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
}

fun assertArenaInfoContains(arenaEiaInfo: ArenaEiaInfo, o: OutcomeType) {
    assertTrue("Expected  ${paeim.toString(arenaEiaInfo)} to contain a SystemSvar with meldingsNr ${o.messageNumber}",
            arenaEiaInfo.eiaData.systemSvar.any { it.meldingsNr.toInt() == o.messageNumber })
}

fun assertOutcomesContain(expected: OutcomeType, outcomes: List<Outcome>) {
    assertTrue("Expected list of outcomes ${outcomes.map { it.outcomeType }} to contain expected",
            outcomes.any { it.outcomeType == expected })
}
