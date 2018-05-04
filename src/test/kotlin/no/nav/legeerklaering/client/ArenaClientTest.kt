package no.nav.legeerklaering.client

import no.nav.legeerklaering.Utils
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Assert
import org.junit.Test


class ArenaClientTest{

    val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")
    val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring
    val outcomeTypes = listOf(OutcomeType.PATIENT_PERSON_NUMBER_NOT_FOUND, OutcomeType.BARN_AV_PASIENT)
    val request = ArenaClient().createArenaEiaInfo(legeerklaring, fellesformat,0,outcomeTypes, "12454")

    @Test
    fun shouldCreateArenaEiaInfo() {

        Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId, request.ediloggId)

    }

}
