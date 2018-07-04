package no.nav.pale.validation

import no.nav.pale.client.Samhandler
import no.nav.pale.client.SamhandlerBregHovedenhet
import no.nav.pale.client.SamhandlerIdent
import no.nav.pale.client.SamhandlerPeriode
import no.nav.pale.client.SamhandlerPraksis
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert
import org.junit.Test
import java.time.LocalDateTime

class PostSarFlowTest {
    val fellesformat = readToFellesformat("/validation/legeerklaeringWithDNR.xml")

    @Test
    fun shouldCreateOutcomeBehandlerNotSar() {
        val samhandler = createSamhandlerListe(
                "Kule Helsetjenester As",
                "aktiv",
                "LE",
               LocalDateTime.now().plusDays(1L),
               LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_NOT_SAR }

        Assert.assertEquals(OutcomeType.BEHANDLER_NOT_SAR, outcome?.outcomeType)
        Assert.assertEquals(1, outcomeList.size)
    }

    @Test
    fun shouldCreateOutcomeTypeAddresseMissingSar() {
        val samhandler = createSamhandlerListe(
                "Kule Helsetjenester As",
                "aktiv",
                "LE",
                LocalDateTime.now().minusDays(1L),
                LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.ADDRESS_MISSING_SAR }

        Assert.assertEquals(OutcomeType.ADDRESS_MISSING_SAR, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeTypeBehandlerTssidEmergencyRoomLEVA() {
        val samhandler = createSamhandlerListe(
                "Kule Helsetjenester As",
                "aktiv",
                "LE",
                LocalDateTime.now().minusDays(1L),
                LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM }

        Assert.assertEquals(OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeBehandlerDNumberButHasValidPersonNumberInSar() {
        val samhandler = createSamhandlerListe(
                "Kule Helsetjenester As",
                "aktiv",
                "LE",
                LocalDateTime.now().minusDays(1L),
                LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR }

        Assert.assertEquals(OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeNoValidTssidPracticeTypeSar() {
        val samhandler = createSamhandlerListe(
                "Kule Helsetjenester As",
                "aktiv",
                "FT",
                LocalDateTime.now().minusDays(1L),
                LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR   }

        Assert.assertEquals(OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR, outcome?.outcomeType)
    }

    @Test
    fun shouldCreateOutcomeUncertianResponseSarShouldVerifiedIfUnder90PercentMatch() {
        val samhandler = createSamhandlerListe(
                "Legevakten Helse As",
                "aktiv",
                "FT",
                LocalDateTime.now().minusDays(1L),
                LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED   }

        Assert.assertEquals(OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED, outcome?.outcomeType)
    }

    @Test
    fun shouldNOTCreateOutcomeUncertianResponseSarShouldVerifiedifOver90PercentMatch() {
        val samhandler = createSamhandlerListe(
                "Kule Helsetjenester As",
                "aktiv",
                "FT",
                LocalDateTime.now().minusDays(1L),
                LocalDateTime.now().plusDays(23L))

        val outcomeList = postSARFlow(fellesformat, samhandler)
        val outcome = outcomeList.find { it.outcomeType == OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED   }

        Assert.assertNotEquals(OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED, outcome?.outcomeType)
    }

    fun createSamhandlerPraksis(praksisGydligfra: LocalDateTime, praksisGyldigtil: LocalDateTime,navn: String, aktiv: String): List<SamhandlerPraksis> {
    val samhandlerPraksisListe = mutableListOf<SamhandlerPraksis>()
        samhandlerPraksisListe.add(
            SamhandlerPraksis(
                    refusjon_type_kode = "231",
                    laerer = "Nope",
                    lege_i_spesialisering = "Nope",
                    tidspunkt_resync_periode = LocalDateTime.now(),
                    tidspunkt_registrert = LocalDateTime.now().minusDays(1L),
                    samh_praksis_status_kode = aktiv,
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
                    navn = navn,
                    ident = "1",
                    samh_praksis_type_kode = "LEVA",
                    samh_id = "1234",
                    samh_praksis_id = "12356",
                    samh_praksis_konto = emptyList(),
                    samh_praksis_periode = createSamhanderPeriode(praksisGydligfra, praksisGyldigtil),
                    samh_praksis_email = emptyList(),
                    samh_praksis_vikar = emptyList()
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

    fun createSamhandlerListe(navn: String, aktiv :String, samhalnderTypekode: String, praksisGydligfra: LocalDateTime, praksisGyldigtil: LocalDateTime): List<Samhandler> {
        val samhandlerListe = mutableListOf<Samhandler>()
        samhandlerListe.add(
                Samhandler(
                        samh_id = "1000288339",
                        navn = "Kule Helsetjenester As",
                        samh_type_kode = samhalnderTypekode,
                        gyldig_fra = LocalDateTime.now().minusDays(2L),
                        gyldig_til = LocalDateTime.now().plusDays(2L),
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
                        samh_praksis = createSamhandlerPraksis(praksisGydligfra,praksisGyldigtil ,navn,aktiv),
                        samh_avtale = emptyList(),
                        samh_direkte_oppgjor_avtale = emptyList(),
                        samh_fbv_godkjent_avd = emptyList(),
                        samh_kommentar = emptyList(),
                        samh_saerskilte_takster = emptyList(),
                        samh_vikar = emptyList(),
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