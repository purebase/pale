package no.nav.pale.mapping

import no.nav.pale.PaleConstant
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultPerson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

object ApprecMapperSpek : Spek({
    val src = defaultFellesformat(defaultPerson())

    describe("Duplicate AppRec") {
        val ff = createApprec(src, ApprecStatus.avvist)
        ff.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.DUPLICATE))
        it("Has the same ediLoggId as the source") {
            ff.mottakenhetBlokk.ediLoggId shouldEqual src.mottakenhetBlokk.ediLoggId
        }
        it("Sets appRec status dn to Avvist") {
            ff.appRec.status.dn shouldEqual ApprecStatus.avvist.dn
        }
        it("Sets appRec error dn to duplicate") {
            ff.appRec.error.first().dn shouldEqual ApprecError.DUPLICATE.dn
        }
        it("Sets appRec error v to duplicate") {
            ff.appRec.error.first().v shouldEqual ApprecError.DUPLICATE.v
        }
        it("Sets appRec error s to duplicate") {
            ff.appRec.error.first().s shouldEqual ApprecError.DUPLICATE.s
        }
    }
    describe("OK AppRec") {
        val ff = createApprec(src, ApprecStatus.ok)
        it("Sets ebRole to ebRoleNav") {
            ff.mottakenhetBlokk.ebRole shouldEqual PaleConstant.ebRoleNav.string
        }
        it("Sets ebService") {
            ff.mottakenhetBlokk.ebService shouldEqual PaleConstant.ebServiceLegemelding.string
        }
        it("Sets ebAction") {
            ff.mottakenhetBlokk.ebAction shouldEqual PaleConstant.ebActionSvarmelding.string
        }
        it("Sets appRec message type") {
            ff.appRec.msgType.v shouldEqual PaleConstant.APPREC.string
        }
        it("Sets appRec miGversion") {
            ff.appRec.miGversion shouldEqual PaleConstant.APPRECVersionV1_0.string
        }
        it("Sets genDate to current date") {
            val now = LocalDateTime.now()
            ff.appRec.genDate.month shouldEqual now.monthValue
            ff.appRec.genDate.day shouldEqual now.dayOfMonth
            ff.appRec.genDate.hour shouldEqual now.hour
        }
        it("Sets appRec id to ediLoggId") {
            ff.appRec.id shouldEqual src.mottakenhetBlokk.ediLoggId
        }
        it("Sets senders appRec sender institution name to receiver organizationName") {
            ff.appRec.sender.hcp.inst.name shouldEqual src.msgHead.msgInfo.receiver.organisation.organisationName
        }
        it("Sets senders appRec institution id to first organization ident id") {
            ff.appRec.sender.hcp.inst.id shouldEqual src.msgHead.msgInfo.receiver.organisation.ident.first().id
        }
        it("Sets senders appRec institution typeId dn to first organization ident typeId dn") {
            ff.appRec.sender.hcp.inst.typeId.dn shouldEqual
                    src.msgHead.msgInfo.receiver.organisation.ident.first().typeId.dn
        }
        it("Sets senders appRec institution typeId v to first organization ident typeId v") {
            ff.appRec.sender.hcp.inst.typeId.v shouldEqual
                    src.msgHead.msgInfo.receiver.organisation.ident.first().typeId.v
        }
        it("Sets senders first additional appRec institution id to second organization ident id") {
            ff.appRec.sender.hcp.inst.additionalId.first().id shouldEqual
                    src.msgHead.msgInfo.receiver.organisation.ident[1].id
        }
        it("Sets senders first additional appRec institution typeId dn to second organization ident typeId dn") {
            ff.appRec.sender.hcp.inst.additionalId.first().type.dn shouldEqual
                    src.msgHead.msgInfo.receiver.organisation.ident[1].typeId.dn
        }
        it("Sets senders first additional appRec institution typeId v to second organization ident typeId v") {
            ff.appRec.sender.hcp.inst.additionalId.first().type.v shouldEqual
                    src.msgHead.msgInfo.receiver.organisation.ident[1].typeId.v
        }
        it("Sets receivers appRec institution name to sender organizationName") {
            ff.appRec.receiver.hcp.inst.name shouldEqual src.msgHead.msgInfo.sender.organisation.organisationName
        }
        it("Sets receivers appRec institution id to first sender organization ident id") {
            ff.appRec.receiver.hcp.inst.id shouldEqual src.msgHead.msgInfo.sender.organisation.ident.first().id
        }
        it("Sets receivers appRec institution typeId dn to first sender organization ident typeId dn") {
            ff.appRec.receiver.hcp.inst.typeId.dn shouldEqual
                    src.msgHead.msgInfo.sender.organisation.ident.first().typeId.dn
        }
        it("Sets receivers appRec institution typeId v to first organization ident typeId v") {
            ff.appRec.receiver.hcp.inst.typeId.v shouldEqual
                    src.msgHead.msgInfo.sender.organisation.ident.first().typeId.v
        }
        it("Sets receivers healthcareProfessionals combined name with no middle name") {
            val noMiddleName = defaultFellesformat(defaultPerson(), doctor = defaultPerson().withPersonnavn(Personnavn()
                    .withFornavn("Fornavn")
                    .withEtternavn("Etternavnsen")))
            createApprec(noMiddleName, ApprecStatus.ok).appRec.receiver.hcp.inst.hcPerson.first().name shouldEqual
                    "Etternavnsen Fornavn"
        }
        it("Sets receivers healthcareProfessionals combined name with middle name") {
            val withMiddleName = defaultFellesformat(defaultPerson(), doctor = defaultPerson().withPersonnavn(Personnavn()
                    .withFornavn("Fornavn")
                    .withMellomnavn("Mellomnavn")
                    .withEtternavn("Etternavnsen")))
            createApprec(withMiddleName, ApprecStatus.ok).appRec.receiver.hcp.inst.hcPerson.first().name shouldEqual
                    "Etternavnsen Fornavn Mellomnavn"
        }
        it("Sets appRec status dn to OK") {
            ff.appRec.status.dn shouldEqual ApprecStatus.ok.dn
        }
        it("Sets appRec status v to OK") {
            ff.appRec.status.v shouldEqual ApprecStatus.ok.v
        }
        it("Sets appRec originalMsgId") {
            ff.appRec.originalMsgId.msgType.dn shouldEqual "Legeerklæring"
        }
        it("Sets appRec originalMsgId") {
            ff.appRec.originalMsgId.msgType.v shouldEqual "LEGEERKL"
        }
        it("Sets appRec genDate as issueDate") {
            ff.appRec.originalMsgId.issueDate shouldEqual src.msgHead.msgInfo.genDate
        }
        it("Sets appRec originalMsgId to msgId") {
            ff.appRec.originalMsgId.id shouldEqual src.msgHead.msgInfo.msgId
        }
    }
    describe("Error AppRec") {
        val ff = createApprec(src, ApprecStatus.avvist)
        ff.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID))
        it("Sets appRec error dn to duplicate") {
            ff.appRec.error.first().dn shouldEqual ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.dn
        }
        it("Sets appRec error v to duplicate") {
            ff.appRec.error.first().v shouldEqual ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v
        }
        it("Sets appRec error s to duplicate") {
            ff.appRec.error.first().s shouldEqual ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.s
        }
    }
})
