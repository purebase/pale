package no.nav.pale.client

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.pale.respondJson
import no.nav.pale.utils.randomPort
import no.nav.pale.utils.readToFellesformat
import no.nav.pale.validation.extractDoctorIdentFromSignature
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

class SarClientTest {

    private lateinit var mockWebserver: ApplicationEngine
    private val mockHttpServerPort = randomPort()
    private val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    val fellesformat = readToFellesformat("/legeerklaering.xml")
    val samhandlerList = createSamhandlerListe()

    @Test
    fun shouldGetSamhandlerNavn() {
        createHttpMock(samhandlerList, OK)

        val sarClient = SarClient(mockHttpServerUrl, "username", "password")
        val samhandler = sarClient.getSamhandler(extractDoctorIdentFromSignature(fellesformat))

        Assert.assertEquals("Kule Helsetjenester As", samhandler.first().navn)
    }

    @Test(expected = IOException::class)
    fun shouldThrowIOExceptionWhenInternalServerError() {
        createHttpMock(samhandlerList, InternalServerError)

        val sarClient = SarClient(mockHttpServerUrl, "username", "password")
         sarClient.getSamhandler(extractDoctorIdentFromSignature(fellesformat))
    }

    private fun createHttpMock(samhandlerList: List<Samhandler>, httpstatskode: HttpStatusCode) {
        mockWebserver = embeddedServer(Netty, mockHttpServerPort) {
            routing {
                get("/rest/sar/samh") {
                    if (httpstatskode.value != OK.value)
                    {
                        call.respond(InternalServerError)
                    }
                    call.respondJson {
                        samhandlerList
                    }
                }
            }
        }.start()
    }

    fun createSamhandlerPraksis(): List<SamhandlerPraksis> {
        val samhandlerPraksisListe = mutableListOf<SamhandlerPraksis>()
        samhandlerPraksisListe.add(
                SamhandlerPraksis(
                        refusjon_type_kode = "231",
                        laerer = "Nope",
                        lege_i_spesialisering = "Nope",
                        tidspunkt_resync_periode = LocalDateTime.now(),
                        tidspunkt_registrert = LocalDateTime.now().minusDays(1L),
                        samh_praksis_status_kode = "aktiv",
                        telefonnr = "89343300",
                        arbeids_kommune_nr = "1201",
                        arbeids_postnr = "0657",
                        arbeids_adresse_linje_1 = "",
                        arbeids_adresse_linje_2 = "Langt vekke",
                        arbeids_adresse_linje_3 = "",
                        arbeids_adresse_linje_4 = "",
                        arbeids_adresse_linje_5 = "",
                        her_id = "12345",
                        post_adresse_linje_1 = "",
                        post_adresse_linje_2 = "",
                        post_adresse_linje_3 = "",
                        post_adresse_linje_4 = "",
                        post_adresse_linje_5 = "",
                        post_kommune_nr = "1201",
                        post_postnr = "",
                        resh_id = "",
                        tss_ident = "1213455",
                        navn = "tull",
                        ident = "1",
                        samh_praksis_type_kode = "LEVA",
                        samh_id = "1345",
                        samh_praksis_id = "12356",
                        samh_praksis_konto = emptyList(),
                        samh_praksis_periode = createSamhanderPeriode(LocalDateTime.now().plusDays(1L), LocalDateTime.now().plusDays(23L)),
                        samh_praksis_email = emptyList()
                )
        )
        return samhandlerPraksisListe
    }

    fun createSamhandlerIdentListe(): List<SamhandlerIdent> {
        val samhandlerIdentListe = mutableListOf<SamhandlerIdent>()
        samhandlerIdentListe.add(
                SamhandlerIdent(
                        samh_id = "1000288339",
                        ident = "1",
                        samh_ident_id = "04030350265",
                        ident_type_kode = "FNR",
                        aktiv_ident = "1"
                )
        )

        samhandlerIdentListe.add(
                SamhandlerIdent(
                        samh_id = "1000288341",
                        ident = "1",
                        samh_ident_id = "74030350265",
                        ident_type_kode = "DNR",
                        aktiv_ident = "1"
                )
        )

        return samhandlerIdentListe
    }

    fun createSamhandlerListe(): List<Samhandler> {
        val samhandlerListe = mutableListOf<Samhandler>()
        samhandlerListe.add(
                Samhandler(
                        samh_id = "12345",
                        navn = "Kule Helsetjenester As",
                        samh_type_kode = "LE",
                        behandling_utfall_kode = "1",
                        unntatt_veiledning = "1",
                        godkjent_manuell_krav = "1",
                        ikke_godkjent_for_refusjon = "1",
                        godkjent_egenandel_refusjon = "1",
                        godkjent_for_fil = "1",
                        breg_hovedenhet = SamhandlerBregHovedenhet(
                                organisasjonsnummer = "12314551",
                                organisasjonsform = "Tull",
                                institusjonellsektorkodekode = "asdasd",
                                naeringskode1kode = "1234",
                                naeringskode2kode = ""
                        ),
                        endringslogg_tidspunkt_siste = LocalDateTime.now(),
                        samh_ident = createSamhandlerIdentListe(),
                        samh_praksis = createSamhandlerPraksis(),
                        samh_avtale = emptyList(),
                        samh_direkte_oppgjor_avtale = emptyList(),
                        samh_email = emptyList()
                )
        )

        return samhandlerListe
    }

    fun createSamhanderPeriode(praksisgyldig_fra: LocalDateTime, praksisgyldig_til: LocalDateTime): List<SamhandlerPeriode> {
        val samhandlerPeriodeListe = mutableListOf<SamhandlerPeriode>()
        samhandlerPeriodeListe.add(
                SamhandlerPeriode(
                        endret_ved_import = "hendelse",
                        sist_endret=  LocalDateTime.now(),
                        slettet= "Nope",
                        gyldig_fra = praksisgyldig_fra,
                        gyldig_til = praksisgyldig_til,
                        samh_praksis_id = "12356",
                        samh_praksis_periode_id =  "1234"

                )
        )

        return samhandlerPeriodeListe
    }
}
