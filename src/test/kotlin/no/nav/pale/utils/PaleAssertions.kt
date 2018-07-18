package no.nav.pale.utils

import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.pale.arenaEiaInfoJaxBContext
import no.nav.pale.mapping.ApprecError
import no.nav.pale.toString
import no.nav.pale.validation.Outcome
import no.nav.pale.validation.OutcomeType
import org.amshove.kluent.should
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotContain
import javax.xml.bind.Marshaller

val paeim = arenaEiaInfoJaxBContext.createMarshaller().apply {
    setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
}

infix fun List<Outcome>.shouldNotContainOutcome(outcomeType: OutcomeType) =
        map { it.outcomeType } shouldNotContain outcomeType
infix fun List<Outcome>.shouldContainOutcome(outcomeType: OutcomeType) =
        map { it.outcomeType } shouldContain outcomeType

infix fun ArenaEiaInfo?.shouldContainOutcome(o: OutcomeType) {
    this!!
    should("${paeim.toString(this)} should contain a SystemSvar with meldingsNr ${o.messageNumber}") {
        eiaData.systemSvar.any { it.meldingsNr.toInt() == o.messageNumber }
    }
}

infix fun AppRec??.shouldContainApprecError(apprecError: ApprecError) {
    this!!
    status.dn shouldEqual "Avvist"
    error[0].s shouldEqual apprecError.s
    error[0].v shouldEqual apprecError.v
}

fun AppRec?.shouldHaveOkStatus() = this!!.status.dn shouldEqual "OK"
