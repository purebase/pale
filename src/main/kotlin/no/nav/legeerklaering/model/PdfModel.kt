package no.nav.legeerklaering.model

import java.time.LocalDateTime

data class Behandlingsvedlegg(
        val type: String,
        val status: String,
        val pasientId: String,
        val ediLoggId: String,
        val msgId: String,
        val generertDato: LocalDateTime,
        val motattNavDato: LocalDateTime,
        val registrertAutomatiskBehandlingDato: LocalDateTime,
        val sender: BehandlingsvedleggSender
)
data class BehandlingsvedleggSender(
        val signaturId: String,
        val signaturNavn: String,
        val avsenderId: String,
        val avsenderNavn: String,
        val tlfNummer: String,
        val organisasjonsId: String,
        val organisasjonsNavn: String,
        val adresse: String,
        val poststed: String,
        val postnummer: String

)

data class Fagmelding(
        val arbeidsvurderingVedSykefravaer: Boolean,
        val arbeidsavklaringsPenger: Boolean,
        val ufoerepensjon: Boolean,
        val pasient: Pasient,
        val plan: Plan,
        val sykdomsOpplysninger: SykdomsOpplysninger,
        val funksjonsOgArbeidsevne: FunksjonsOgArbeidsevne,
        val merknadAvvist: List<Merknad>,
        val merknadManuellBehandling: List<Merknad>,
        val merknadOppfoelging: List<Merknad>,
        val merknadNotis: List<Merknad>,
        val documentInfo: List<Merknad>
)

data class Pasient(
        val fornavn: String,
        val etternavn: String,
        val foedselsnummer: String,
        val navKontor: String,
        val adresse: String,
        val postnummer: Int,
        val poststed: String,
        val yrke: String
)

data class SykdomsOpplysninger (
    val hoveddiagnose: Diagnose,
    val bidiagnose: List<Diagnose>,
    val arbeidsufoerFra: String,
    val sykdomsHistorie: String,
    val statusPresens: String,
    val boerNavKontoretVurdereOmDetErEnYrkesskade: Boolean
)

data class Diagnose(
        val tekst: String,
        val koder: List<String>
)

data class Plan(
        val utredning: Henvisning,
        val behandling: Henvisning,
        val utredningsplan: String,
        val behandlingsplan: String,
        val vurderingAvTidligerePlan: String,
        val naarSpoerreOmNyeLegeopplysninger: String,
        val videreBehandlingIkkeAktuellGrunn: String
)

data class Henvisning(
        val tekst: String,
        val dato: LocalDateTime,
        val antattVentetIUker: Int
)

data class FunksjonsOgArbeidsevne(
        val behov: Boolean,
        val kjoepAvHelsetjenester: Boolean,
        val reisetilskudd: Boolean,
        val aktivSykMelding: Boolean,
        val hjelpemidlerArbeidsplassen: Boolean,
        val arbeidsavklaringsPenger: Boolean,
        val friskemeldingTilArbeidsformidling: Boolean,
        val andreTiltak: String,
        val naermereOpplysninger: String,
        val tekst: String
)

data class Merknad(
        val tekst: String,
        val number: Int
)
