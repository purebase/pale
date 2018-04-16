package no.nav.legeerklaering

import org.junit.Test


class CreateArenaEiaInfo{

    @Test
    fun shouldCreateArenaEiaInfo() {

        val fellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val legeerklaring = Utils.readToLegerklearing("/legeerklaeringFagmelding.xml")

        LegeerklaeringApplication().createArenaEiaInfo(legeerklaring, fellesformat)

    }

}