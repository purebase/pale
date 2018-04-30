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
        val merknadAvvist: List<Merknad>,
        val merknadManuellBehandling: List<Merknad>,
        val merknadOppfoelging: List<Merknad>,
        val merknadNotis: List<Merknad>,
        val documentInfo: List<Merknad>
)

data class DocumentInfo(
        val type: String,
        val status: String,
        val pasientId: String
)

data class Sporing(
        val ediLoggId: String
)

data class Merknad(
        val tekst: String,
        val number: Int
)
