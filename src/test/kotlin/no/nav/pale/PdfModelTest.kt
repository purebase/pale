package no.nav.pale

import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.mapping.mapFellesformatToFagmelding
import no.nav.pale.model.*
import no.nav.pale.utils.readToFellesformat
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*

class PdfModelTest {

    @Test
    fun testFagmeldingJsonModel() {
        val pdfModel = Fagmelding(
                arbeidsvurderingVedSykefravaer = true,
                arbeidsavklaringsPenger = true,
                yrkesrettetAttfoering = false,
                ufoerepensjon = true,
                pasient = Pasient(
                        fornavn = "Test",
                        mellomnavn = "Testerino",
                        etternavn = "Testsen",
                        foedselsnummer = "0123456789",
                        navKontor = "NAV Stockholm",
                        adresse = "Oppdiktet veg 99",
                        postnummer = 9999,
                        poststed = "Stockholm",
                        yrke = "Taco spesialist",
                        arbeidsgiver = Arbeidsgiver(
                                navn = "NAV IKT",
                                adresse = "Sannergata 2",
                                postnummer = 557,
                                poststed = "Oslo"
                        )
                ),
                sykdomsOpplysninger = SykdomsOpplysninger(
                        hoveddiagnose = Diagnose(
                                tekst = "Tekst",
                                kode = "test"
                        ),
                        bidiagnose = listOf(),
                        arbeidsufoerFra = ZonedDateTime.now().minusDays(3),
                        sykdomsHistorie = "Tekst",
                        statusPresens = "Tekst",
                        boerNavKontoretVurdereOmDetErEnYrkesskade = true
                ),
                plan = Plan(
                        utredning = null,
                        behandling = Henvisning(
                                tekst = "2 timer i uken med svømming",
                                dato = ZonedDateTime.now(),
                                antattVentetIUker = 1
                        ),
                        utredningsplan = "Tekst",
                        behandlingsplan = "Tekst",
                        vurderingAvTidligerePlan = "Tekst",
                        naarSpoerreOmNyeLegeopplysninger = "Tekst",
                        videreBehandlingIkkeAktuellGrunn = "Tekst"
                ),
                forslagTilTiltak = ForslagTilTiltak(
                        behov = true,
                        kjoepAvHelsetjenester = true,
                        reisetilskudd = false,
                        aktivSykMelding = false,
                        hjelpemidlerArbeidsplassen = true,
                        arbeidsavklaringsPenger = true,
                        friskemeldingTilArbeidsformidling = false,
                        andreTiltak = "Trenger taco i lunsjen",
                        naermereOpplysninger = "Tacoen må bestå av ordentlige råvarer",
                        tekst = "Pasienten har store problemer med fordøying av annen mat enn Taco"

                ),
                funksjonsOgArbeidsevne = FunksjonsOgArbeidsevne(
                        iIntektsgivendeArbeid = false,
                        hjemmearbeidende = false,
                        student = false,
                        annetArbeid = "Reisende taco tester",
                        kravTilArbeid = "Kun taco i kantina",
                        kanGjenopptaTidligereArbeid = true,
                        kanGjenopptaTidligereArbeidNaa = true,
                        kanGjenopptaTidligereArbeidEtterBehandling = true,
                        kanTaAnnetArbeid = true,
                        kanTaAnnetArbeidNaa = true,
                        kanTaAnnetArbeidEtterBehandling = true,
                        kanIkkeINaaverendeArbeid = "Spise annen mat enn Taco",
                        kanIkkeIAnnetArbeid = "Spise annen mat enn Taco"
                ),
                prognose = Prognose(
                        vilForbedreArbeidsevne = true,
                        anslaatVarighetSykdom = "1 uke",
                        anslaatVarighetFunksjonsNedsetting = "2 uker",
                        anslaatVarighetNedsattArbeidsevne = "4 uker"
                ),
                aarsaksSammenheng = "Funksjonsnedsettelsen har stor betydning for at arbeidsevnen er nedsatt",
                andreOpplysninger = "Tekst",
                kontakt = Kontakt(
                        skalKontakteBehandlendeLege = true,
                        skalKontakteArbeidsgiver = true,
                        skalKontakteBasisgruppe = false,
                        kontakteAnnenInstans = null,
                        oenskesKopiAvVedtak = true
                ),
                pasientenBurdeIkkeVite = null,
                signatur = Signatur(
                        dato = ZonedDateTime.now().minusDays(1),
                        navn = "Lege Legesen",
                        adresse = "Legeveien 33",
                        postnummer = 9999,
                        poststed = "Stockholm",
                        signatur = "Lege Legesen",
                        tlfNummer = "98765432"
                )
        )
        println(objectMapper.writeValueAsString(pdfModel))
    }

    @Test
    fun testFellesformatToFagmelding() {
        val fellesformat = readToFellesformat("/legeerklaering.xml")
        // val pdfModel = mapFellesformatToFagmelding(fellesformat)
        val pdfModel = mapFellesformatToFagmelding(defaultFellesformat())

        println(TimeZone.getDefault().id)
        println(pdfModel.sykdomsOpplysninger.arbeidsufoerFra?.zone?.id)
        println(pdfModel.signatur.dato.zone.id)
        println(pdfModel.sykdomsOpplysninger.arbeidsufoerFra)
        println(objectMapper.writeValueAsString(pdfModel))
    }
}
