package no.nav.pale.mapping

import no.nav.pale.model.*
import no.nav.pale.model.Kontakt
import no.nav.pale.model.Pasient
import no.nav.pale.model.Prognose
import no.nav.pale.validation.extractLegeerklaering
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.*
import no.nav.model.msghead.MsgHeadCS
import java.time.ZonedDateTime

fun mapFellesformatToFagmelding(fellesformat: EIFellesformat): Fagmelding {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val plan = legeerklaering.planUtredBehandle
    val forslagTiltak = legeerklaering.forslagTiltak
    val typeLegeerklaering = legeerklaering.legeerklaringGjelder[0].typeLegeerklaring.toInt()
    val funksjonsevne = legeerklaering.vurderingFunksjonsevne
    val prognose = legeerklaering.prognose
    val healthcareProfessional = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional

    return Fagmelding(
            arbeidsvurderingVedSykefravaer = typeLegeerklaering == LegeerklaeringType.Arbeidsevnevurdering.type,
            arbeidsavklaringsPenger = typeLegeerklaering == LegeerklaeringType.Arbeidsavklaringspenger.type,
            yrkesrettetAttfoering = typeLegeerklaering == LegeerklaeringType.YrkesrettetAttfoering.type,
            ufoerepensjon = typeLegeerklaering == LegeerklaeringType.Ufoerepensjon.type,
            pasient = legeerklaeringToPasient(legeerklaering),
            sykdomsOpplysninger = mapLegeerklaeringToSykdomDiagnose(legeerklaering.diagnoseArbeidsuforhet),
            plan = Plan(
                    utredning = plan.henvistUtredning?.let {
                        Henvisning(
                                tekst = it.spesifikasjon,
                                dato = it.henvistDato.toGregorianCalendar().toZonedDateTime(),
                                antattVentetIUker = it.antattVentetid.toInt()
                        )
                    },
                    behandling = plan.henvistBehandling?.let {
                        Henvisning(
                                tekst = it.spesifikasjon,
                                dato = it.henvistDato.toGregorianCalendar().toZonedDateTime(),
                                antattVentetIUker = it.antattVentetid.toInt()
                        )
                    },
                    utredningsplan = plan.utredningsPlan,
                    behandlingsplan = plan.behandlingsPlan,
                    vurderingAvTidligerePlan = plan.nyVurdering,
                    naarSpoerreOmNyeLegeopplysninger = plan.nyeLegeopplysninger,
                    videreBehandlingIkkeAktuellGrunn = plan.ikkeVidereBehandling
            ),
            forslagTilTiltak = ForslagTilTiltak(
                    behov = forslagTiltak.aktueltTiltak.isEmpty(),
                    kjoepAvHelsetjenester = TypeTiltak.KjoepHelsetjenester in forslagTiltak.aktueltTiltak,
                    reisetilskudd = TypeTiltak.Reisetilskudd in forslagTiltak.aktueltTiltak,
                    aktivSykMelding = TypeTiltak.AktivSykemelding in forslagTiltak.aktueltTiltak,
                    hjelpemidlerArbeidsplassen = TypeTiltak.HjelpemidlerArbeidsplass in forslagTiltak.aktueltTiltak,
                    arbeidsavklaringsPenger = TypeTiltak.Arbeidsavklaringspenger in forslagTiltak.aktueltTiltak,
                    friskemeldingTilArbeidsformidling = TypeTiltak.FriskemeldingTilArbeidsformidling in forslagTiltak.aktueltTiltak,
                    andreTiltak = forslagTiltak.aktueltTiltak.find { it.typeTiltak == TypeTiltak.AndreTiltak }?.hvilkeAndreTiltak,
                    naermereOpplysninger = forslagTiltak.opplysninger,
                    tekst = forslagTiltak.begrensningerTiltak ?: forslagTiltak.begrunnelseIkkeTiltak
            ),
            funksjonsOgArbeidsevne = FunksjonsOgArbeidsevne(
                    iIntektsgivendeArbeid = ArbeidssituasjonType.InntektsgivendeArbeid in funksjonsevne.arbeidssituasjon,
                    hjemmearbeidende = ArbeidssituasjonType.Hjemmearbeidende in funksjonsevne.arbeidssituasjon,
                    student = ArbeidssituasjonType.Student in funksjonsevne.arbeidssituasjon,
                    annetArbeid = funksjonsevne.arbeidssituasjon.find { it.arbeidssituasjon.toInt() == ArbeidssituasjonType.Annet.type }?.annenArbeidssituasjon,
                    kravTilArbeid = funksjonsevne.kravArbeid,
                    kanGjenopptaTidligereArbeid = funksjonsevne.vurderingArbeidsevne.gjenopptaArbeid.toInt() == 1,
                    kanGjenopptaTidligereArbeidNaa = funksjonsevne.vurderingArbeidsevne.narGjenopptaArbeid.toInt() == 1,
                    kanGjenopptaTidligereArbeidEtterBehandling = funksjonsevne.vurderingArbeidsevne.narGjenopptaArbeid.toInt() == 2,
                    kanTaAnnetArbeid = funksjonsevne.vurderingArbeidsevne.taAnnetArbeid.toInt() == 1,
                    kanTaAnnetArbeidNaa = funksjonsevne.vurderingArbeidsevne.narTaAnnetArbeid.toInt() == 1,
                    kanTaAnnetArbeidEtterBehandling = funksjonsevne.vurderingArbeidsevne.narTaAnnetArbeid.toInt() == 2,
                    kanIkkeINaaverendeArbeid = funksjonsevne.vurderingArbeidsevne.ikkeGjore,
                    kanIkkeIAnnetArbeid = funksjonsevne.vurderingArbeidsevne.hensynAnnetYrke
            ),
            prognose = Prognose(
                    vilForbedreArbeidsevne = prognose.bedreArbeidsevne.toInt() == 1,
                    anslaatVarighetSykdom = prognose.antattVarighet,
                    anslaatVarighetFunksjonsNedsetting = prognose.varighetFunksjonsnedsettelse,
                    anslaatVarighetNedsattArbeidsevne = prognose.varighetNedsattArbeidsevne
            ),
            aarsaksSammenheng = legeerklaering.arsakssammenhengLegeerklaring,
            andreOpplysninger = legeerklaering.andreOpplysninger?.opplysning,
            kontakt = Kontakt(
                    skalKontakteBehandlendeLege = KontaktType.BehandlendeLege in legeerklaering.kontakt,
                    skalKontakteArbeidsgiver = KontaktType.Arbeidsgiver in legeerklaering.kontakt,
                    skalKontakteBasisgruppe = KontaktType.Basisgruppe in legeerklaering.kontakt,
                    kontakteAnnenInstans = legeerklaering.kontakt.find { it.kontakt.toInt() == KontaktType.AnnenInstans.type }?.annenInstans,
                    oenskesKopiAvVedtak = legeerklaering.andreOpplysninger?.onskesKopi?.let { it.toInt() == 1 } ?: false
            ),
            pasientenBurdeIkkeVite = legeerklaering.forbeholdLegeerklaring.borTilbakeholdes,
            signatur = Signatur(
                    dato = ZonedDateTime.now(),
                    navn = "${healthcareProfessional.familyName}, ${healthcareProfessional.givenName} ${healthcareProfessional.middleName}",
                    //adresse = healthcareProfessional.address.streetAdr,
                    //postnummer = healthcareProfessional.address.postalCode.toInt(),
                    //poststed = healthcareProfessional.address.city,
                    adresse = fellesformat.msgHead.msgInfo.sender.organisation.address.streetAdr,
                    postnummer = fellesformat.msgHead.msgInfo.sender.organisation.address.postalCode.toInt(),
                    poststed = fellesformat.msgHead.msgInfo.sender.organisation.address.city,
                    signatur = "",
                    tlfNummer = healthcareProfessional.teleCom.find { it.typeTelecom in PhoneType }?.teleAddress?.v
            )
    )
}

enum class PhoneType(val v: String, val dn: String) {
    MainPhone("HP", "Hovedtelefon"),
    MobilePhone("MC", "Mobiltelefon"),
    WorkPhone("WP", "Arbeidsplass");
    companion object {
        operator fun contains(type: MsgHeadCS): Boolean =
                values().any { it.v == type.v }
    }
}


fun mapEnkeltDiagnoseToDiagnose(enkeltdiagnose: Enkeltdiagnose): Diagnose =
        Diagnose(tekst = enkeltdiagnose.diagnose, kode = enkeltdiagnose.kodeverdi)

fun mapLegeerklaeringToSykdomDiagnose(diagnose: DiagnoseArbeidsuforhet): SykdomsOpplysninger = SykdomsOpplysninger(
        hoveddiagnose = mapEnkeltDiagnoseToDiagnose(diagnose.diagnoseKodesystem.enkeltdiagnose[0]),
        bidiagnose = diagnose.diagnoseKodesystem.enkeltdiagnose.drop(1).map { mapEnkeltDiagnoseToDiagnose(it) },
        arbeidsufoerFra = diagnose.arbeidsuforFra?.toGregorianCalendar()?.toZonedDateTime(),
        sykdomsHistorie = diagnose.symptomerBehandling,
        statusPresens = diagnose.statusPresens,
        boerNavKontoretVurdereOmDetErEnYrkesskade = diagnose.vurderingYrkesskade.borVurderes.toInt() == 1
)


fun legeerklaeringToPasient(legeerklaering: Legeerklaring): Pasient {
    val patient = legeerklaering.pasientopplysninger.pasient
    val postalAddress = patient.arbeidsforhold.virksomhet.virksomhetsAdr.postalAddress[0]
    return Pasient(
            fornavn = patient.navn.fornavn,
            mellomnavn = patient.navn.mellomnavn,
            etternavn = patient.navn.etternavn,
            foedselsnummer = patient.fodselsnummer,
            navKontor = patient.trygdekontor,
            adresse = patient.personAdr[0].postalAddress[0].streetAddress,
            postnummer = patient.personAdr[0].postalAddress[0].postalCode.toInt(),
            poststed = patient.personAdr[0].postalAddress[0].city,
            yrke = patient.arbeidsforhold.yrkesbetegnelse,
            arbeidsgiver = Arbeidsgiver(
                    navn = patient.arbeidsforhold.virksomhet.virksomhetsBetegnelse,
                    adresse = postalAddress.streetAddress,
                    postnummer = postalAddress.postalCode.let {
                        if (it == null || it.isEmpty()) null else it.toInt()
                    },
                    poststed = postalAddress.city
            )
    )
}

enum class TypeTiltak(val typeTiltak: Int) {
    KjoepHelsetjenester(1),
    Reisetilskudd(2),
    AktivSykemelding(3),
    HjelpemidlerArbeidsplass(4),
    Arbeidsavklaringspenger(5),
    FriskemeldingTilArbeidsformidling(6),
    AndreTiltak(7)
}

operator fun Iterable<AktueltTiltak>.contains(typeTiltak: TypeTiltak) =
        any { it.typeTiltak.toInt() == typeTiltak.typeTiltak }

enum class LegeerklaeringType(val type: Int) {
    Arbeidsevnevurdering(1),
    Arbeidsavklaringspenger(2),
    YrkesrettetAttfoering(3),
    Ufoerepensjon(4)
}

enum class ArbeidssituasjonType(val type: Int) {
    InntektsgivendeArbeid(1),
    Hjemmearbeidende(2),
    Student(3),
    Annet(4)
}

operator fun Iterable<Arbeidssituasjon>.contains(arbeidssituasjonType: ArbeidssituasjonType): Boolean =
        any { it.arbeidssituasjon.toInt() == arbeidssituasjonType.type }

enum class KontaktType(val type: Int) {
    BehandlendeLege(1),
    Arbeidsgiver(2),
    Basisgruppe(4),
    AnnenInstans(5)
}

operator fun Iterable<no.nav.model.pale.Kontakt>.contains(kontaktType: KontaktType): Boolean =
        any { it.kontakt.toInt() == kontaktType.type }
