package no.nav.legeerklaering

import org.junit.Test


class CreatearchiveMessage{

    @Test
    fun shouldCreateArenaEiaInfo() {

        val fellesformat = Utils.readToFellesformat("/readToLegerklearing.xml")
        val legeerklaring = Utils.readToLegerklearing("/legeerklaeringFagmelding.xml")

        LegeerklaeringApplication().archiveMessage(legeerklaring, fellesformat)

        //should assert TODO
    }

}