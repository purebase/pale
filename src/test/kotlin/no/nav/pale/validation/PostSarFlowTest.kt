package no.nav.pale.validation

import no.nav.pale.client.SamhandlerIdent
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
        val samhanlderidentListe = createSamhandlerIdentListe()

        val outcomeList = postSARFlow(samhandlerPraksis, samhanlderidentListe)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.ADDRESS_MISSING_SAR }

        Assert.assertEquals(OutcomeType.ADDRESS_MISSING_SAR, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerTssidEmergencyRoomLEVA() {
        val samhandlerPraksis = createSamhandlerPraksis()
        val samhanlderidentListe = createSamhandlerIdentListe()

        val outcomeList = postSARFlow(samhandlerPraksis, samhanlderidentListe)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM }

        Assert.assertEquals(OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeBehandlerDNumberButHasValidPersonNumberInSar() {
        val samhandlerPraksis = createSamhandlerPraksis()
        val samhanlderidentListe = createSamhandlerIdentListe()

        val outcomeList = postSARFlow(samhandlerPraksis, samhanlderidentListe)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR }

        Assert.assertEquals(OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR, outcome?.outcomeType)
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

}