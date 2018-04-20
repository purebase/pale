package no.nav.legeerklaering

import no.nav.model.legeerklaering.Legeerklaring
import org.junit.Test


class CreatearchiveMessage{

    @Test
    fun shouldCreateArenaEiaInfo() {

        val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val legeerklaring = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

        LegeerklaeringApplication().archiveMessage(legeerklaring, fellesformat)

        //should assert TODO
    }

}
