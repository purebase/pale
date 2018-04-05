package no.nav.legeerklaering

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*
import javax.xml.datatype.DatatypeFactory

class CreateApprecTest {

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkAvsender() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.mottakenhetBlokk.avsender, apprecFellesformat.mottakenhetBlokk.avsender)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEdiLoggId() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.mottakenhetBlokk.ediLoggId, apprecFellesformat.mottakenhetBlokk.ediLoggId)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEbRole() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.mottakenhetBlokk.ebRole, apprecFellesformat.mottakenhetBlokk.ebRole)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEbService() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.mottakenhetBlokk.ebService, apprecFellesformat.mottakenhetBlokk.ebService)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEbAction() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.mottakenhetBlokk.ebAction, apprecFellesformat.mottakenhetBlokk.ebAction)
    }

    @Test
    fun shouldCreateApprecOKWithMsgTypeDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.msgType.dn, apprecFellesformat.appRec.msgType.dn)
    }

    @Test
    fun shouldCreateApprecOKWithMIGversion() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.miGversion, apprecFellesformat.appRec.miGversion)
    }

    @Test
    fun shouldCreateApprecOKWithMGenDate() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedCurrentDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar())

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedCurrentDate.day, apprecFellesformat.appRec.genDate.day)
        assertEquals(expectedCurrentDate.hour, apprecFellesformat.appRec.genDate.hour)
        assertEquals(expectedCurrentDate.minute, apprecFellesformat.appRec.genDate.minute)
    }

    @Test
    fun shouldCreateApprecOKWithApprecId() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.id, apprecFellesformat.appRec.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstName() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.name, apprecFellesformat.appRec.sender.hcp.inst.name)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstId() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.id, apprecFellesformat.appRec.sender.hcp.inst.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstTypeIdDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.typeId.dn, apprecFellesformat.appRec.sender.hcp.inst.typeId.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstTypeIdV() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.typeId.v, apprecFellesformat.appRec.sender.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstId() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.additionalId[0].id, apprecFellesformat.appRec.sender.hcp.inst.additionalId[0].id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstTypeDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.additionalId[0].type.dn, apprecFellesformat.appRec.sender.hcp.inst.additionalId[0].type.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstTypeV() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.sender.hcp.inst.additionalId[0].type.v, apprecFellesformat.appRec.sender.hcp.inst.additionalId[0].type.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstName() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.receiver.hcp.inst.name, apprecFellesformat.appRec.receiver.hcp.inst.name)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstId() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.receiver.hcp.inst.id, apprecFellesformat.appRec.receiver.hcp.inst.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstTypeIdDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.receiver.hcp.inst.typeId.dn, apprecFellesformat.appRec.receiver.hcp.inst.typeId.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstTypeIdVIs1() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.receiver.hcp.inst.typeId.v, apprecFellesformat.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecOKWithStatusDnIsOK() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.status.dn, apprecFellesformat.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithApprecReceiverHCPInstTypeIdVIs2() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecDuplikat.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "duplikat")

        assertEquals(expectedApprecFellesformat.appRec.receiver.hcp.inst.typeId.v, apprecFellesformat.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecDuplikatWithStatusDnIsAvist() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecDuplikat.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "duplikat")

        assertEquals(expectedApprecFellesformat.appRec.status.dn, apprecFellesformat.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErrosDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecDuplikat.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "duplikat")

        assertEquals(expectedApprecFellesformat.appRec.error.get(0).dn, apprecFellesformat.appRec.error.get(0).dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErrosV() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecDuplikat.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "duplikat")

        assertEquals(expectedApprecFellesformat.appRec.error.get(0).v, apprecFellesformat.appRec.error.get(0).v)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErross() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecDuplikat.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "duplikat")

        assertEquals(expectedApprecFellesformat.appRec.error.get(0).s, apprecFellesformat.appRec.error.get(0).s)
    }

    @Test
    fun shouldCreateApprecAvistWithApprecReceiverHCPInstTypeIdVIs2() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecAvist.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "avvist")

        assertEquals(expectedApprecFellesformat.appRec.receiver.hcp.inst.typeId.v, apprecFellesformat.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecDAvistWithStatusDnIsAvist() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecAvist.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "avvist")

        assertEquals(expectedApprecFellesformat.appRec.status.dn, apprecFellesformat.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecRAvistWithErroDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecAvist.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "avvist")

        assertEquals(expectedApprecFellesformat.appRec.error.get(0).dn, apprecFellesformat.appRec.error.get(0).dn)
    }

    @Test
    fun shouldCreateApprecAvistWithErroV() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecAvist.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "avvist")

        assertEquals(expectedApprecFellesformat.appRec.error.get(0).v, apprecFellesformat.appRec.error.get(0).v)
    }

    @Test
    fun shouldCreateApprecAvistWithErroS() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecAvist.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "avvist")

        assertEquals(expectedApprecFellesformat.appRec.error.get(0).s, apprecFellesformat.appRec.error.get(0).s)
    }

    @Test
    fun shouldCreateApprecOKWithStatusV() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.status.v, apprecFellesformat.appRec.status.v)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdMsgTypeDn() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.originalMsgId.msgType.dn, apprecFellesformat.appRec.originalMsgId.msgType.dn)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdMsgTypeV() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.originalMsgId.msgType.v, apprecFellesformat.appRec.originalMsgId.msgType.v)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdIssueDate() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.originalMsgId.issueDate, apprecFellesformat.appRec.originalMsgId.issueDate)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdId() {
        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val expectedApprecFellesformat = Utils.readToFellesformat("/legeerklaeringApprecOK.xml")

        val apprecFellesformat = createApprec(inputMeldingFellesformat, "ok")

        assertEquals(expectedApprecFellesformat.appRec.originalMsgId.id, apprecFellesformat.appRec.originalMsgId.id)
    }

}