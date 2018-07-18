package no.nav.pale.client

import no.nav.pale.validation.extractLegeerklaering
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.pale.PaleConstant
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.getHCPFodselsnummer
import no.nav.pale.mapping.formatName
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ArenaClientSpek : Spek({
    val tssId = "12454"
    val navOffice = "0301"
    describe("Default fellesformat") {
        val fellesformat = defaultFellesformat(defaultPerson())
        val request: ArenaEiaInfo = createArenaInfo(fellesformat, tssId, null, navOffice)
        it("Sets EdiLoggId") {
            request.ediloggId shouldEqual fellesformat.mottakenhetBlokk.ediLoggId
        }
        it("Sets hendelse status") {
            request.hendelseStatus shouldEqual PaleConstant.tilvurdering.string
        }
        it("Sets version") {
            request.version shouldEqual "2.0"
        }
        it("Sets schema type") {
            request.skjemaType shouldEqual PaleConstant.LE.string
        }
        it("Sets patients person number") {
            request.pasientData.fnr shouldEqual
                    extractLegeerklaering(fellesformat).pasientopplysninger.pasient.fodselsnummer
        }
        it("Sets is restricted") {
            request.pasientData.isSperret shouldEqual
                    (extractLegeerklaering(fellesformat).forbeholdLegeerklaring.tilbakeholdInnhold.toInt() == 2)
        }
        it("Sets NAV office") {
            request.pasientData.tkNummer shouldEqual navOffice
        }
        it("Sets doctors name") {
            request.legeData.navn shouldEqual
                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.formatName()
        }
        it("Sets doctors person number") {
            request.legeData.fnr shouldEqual getHCPFodselsnummer(fellesformat)
        }
        it("Sets doctors TSS id") {
            request.legeData.tssid shouldEqual tssId
        }
    }
    describe("Fellesformats with different legeerklæring types") {
        it("Sets folder type to SP on legeerklæring type 1") {
            val fellesformat = defaultFellesformat(defaultPerson())
            extractLegeerklaering(fellesformat).legeerklaringGjelder.first().typeLegeerklaring = 1.toBigInteger()
            createArenaInfo(fellesformat, tssId, null, navOffice).mappeType shouldEqual
                    PaleConstant.mappetypeSP.string
        }
        it("Sets folder type to RP on legeerklæring type 2") {
            val fellesformat = defaultFellesformat(defaultPerson())
            extractLegeerklaering(fellesformat).legeerklaringGjelder.first().typeLegeerklaring = 2.toBigInteger()
            createArenaInfo(fellesformat, tssId, null, navOffice).mappeType shouldEqual
                    PaleConstant.mappetypeRP.string
        }
        it("Sets folder type to YA on legeerklæring type 3") {
            val fellesformat = defaultFellesformat(defaultPerson())
            extractLegeerklaering(fellesformat).legeerklaringGjelder.first().typeLegeerklaring = 3.toBigInteger()
            createArenaInfo(fellesformat, tssId, null, navOffice).mappeType shouldEqual
                    PaleConstant.mappetypeYA.string
        }
        it("Sets folder type to RA on legeerklæring type 4") {
            val fellesformat = defaultFellesformat(defaultPerson())
            extractLegeerklaering(fellesformat).legeerklaringGjelder.first().typeLegeerklaring = 4.toBigInteger()
            createArenaInfo(fellesformat, tssId, null, navOffice).mappeType shouldEqual
                    PaleConstant.mappetypeUP.string
        }
    }
})
