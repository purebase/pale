package no.nav.pale.mapping

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object FellesformatToFagmeldingSpek : Spek({
    describe("Defaualt fellesformat") {
        it("Does not cause an exception when converting to fagmelding") {
            mapFellesformatToFagmelding(defaultFellesformat(defaultPerson()))
        }
    }
})
