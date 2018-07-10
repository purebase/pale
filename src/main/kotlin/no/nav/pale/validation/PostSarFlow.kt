package no.nav.pale.validation

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.SamhandlerPraksisMatch
import no.nav.pale.calculatePercentageStringMatch
import no.nav.pale.client.Samhandler
import no.nav.pale.findBestSamhandlerPraksis
import kotlin.math.roundToInt

fun postSARFlow(fellesformat: EIFellesformat, samhandler: List<Samhandler>): List<Outcome> {
    val outcome = mutableListOf<Outcome>()
    val orgName = extractSenderOrganisationName(fellesformat)

    val samhandlerPraksis = findBestSamhandlerPraksis(samhandler, fellesformat)?.samhandlerPraksis

    if (samhandlerPraksis == null) {
        outcome += OutcomeType.BEHANDLER_NOT_SAR.toOutcome()
        return outcome
    }

    val samhandlerPraksisMatch = SamhandlerPraksisMatch(samhandlerPraksis, calculatePercentageStringMatch(samhandlerPraksis.navn, orgName))
    if (samhandlerPraksisMatch.percentageMatch < 0.9) {
        outcome += OutcomeType.UNCERTAIN_RESPONSE_SAR_SHOULD_VERIFIED.toOutcome((samhandlerPraksisMatch.percentageMatch.times(100.0).roundToInt()))
    }

    if (samhandlerPraksis.arbeids_adresse_linje_1 == null || samhandlerPraksis.arbeids_adresse_linje_1.isEmpty()) {
        outcome += OutcomeType.ADDRESS_MISSING_SAR
    }

    if (samhandlerPraksis.samh_praksis_type_kode in arrayOf("LEVA", "LEKO")) {
        outcome += OutcomeType.BEHANDLER_TSSID_EMERGENCY_ROOM
    }

    val samhandlerIdentFnr = samhandler.flatMap { it.samh_ident }
            .filter { it.ident_type_kode == "FNR" }
            .filter { it.aktiv_ident == "1" }

    if (isDNR(extractDoctorIdentFromSignature(fellesformat)) && samhandlerIdentFnr.isNotEmpty()) {
        outcome += OutcomeType.BEHANDLER_D_NUMBER_BUT_HAS_VALID_PERSON_NUMBER_IN_SAR
    }

    if (samhandler.none { it.samh_type_kode in arrayOf("LE", "KI", "TL", "MT") }) {
        outcome += OutcomeType.NO_VALID_TSSID_PRACTICE_TYPE_SAR
    }

    collectFlowStatistics(outcome)
    return outcome
}
