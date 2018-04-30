package no.nav.legeerklaering.model

data class Behandlingsvedlegg(
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
