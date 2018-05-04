package no.nav.legeerklaering.client

import no.nav.legeerklaering.Utils
import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Assert
import org.junit.Test


class ArenaClientTest{

    val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")
    val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring
    val request = ArenaClient().createArenaEiaInfo(legeerklaring, fellesformat)

    @Test
    fun shouldCreateArenaEiaInfo() {

        Assert.assertEquals(fellesformat.mottakenhetBlokk.ediLoggId, request.ediloggId)

    }

}
