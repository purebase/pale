package no.nav.pale.validation

import no.nav.pale.client.SamhandlerPraksis
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Test
import java.time.LocalDateTime

class PostSarFlowTest {
    val fellesformat = readToFellesformat("/legeerklaering.xml")

    @Test
    fun shouldCreateOutcomeTypeAddresseMissingSar() {
        val samhandlerPraksis = createSamhandlerPraksis()

        val outcomeList = postSARFlow(samhandlerPraksis)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.ADDRESS_MISSING_SAR }

        Assert.assertEquals(OutcomeType.ADDRESS_MISSING_SAR, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerTssidEmergencyRoomLEVA() {
        val samhandlerPraksis = createSamhandlerPraksis()

        val outcomeList = postSARFlow(samhandlerPraksis)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM }

        Assert.assertEquals(OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM, outcome?.outcomeType)
    }

    fun createSamhandlerPraksis(): SamhandlerPraksis =  SamhandlerPraksis(
    refusjon_type_kode = "231",
    laerer = "Nope",
    lege_i_spesialisering = "Nope",
    tidspunkt_resync_periode = LocalDateTime.now(),
    tidspunkt_registrert = LocalDateTime.now().minusDays(1L),
    samh_praksis_status_kode = "Aktiv",
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
    navn = "Kule helsetjenester AS",
    ident = "1",
    samh_praksis_type_kode = "LEVA",
    samh_id = "1234",
    samh_praksis_id = "12356",
    samh_praksis_konto = emptyList(),
    samh_praksis_periode = emptyList(),
    samh_praksis_email = emptyList(),
    samh_praksis_vikar = emptyList()
    )

}