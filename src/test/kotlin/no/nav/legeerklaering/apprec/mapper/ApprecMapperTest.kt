package no.nav.legeerklaering.apprec.mapper

import no.nav.legeerklaering.Utils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*
import javax.xml.datatype.DatatypeFactory

class ApprecMapperTest {

    val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
    val expectedApprecFellesformatOK = Utils.readToFellesformat("/apprec/legeerklaeringApprecOK.xml")
    val expectedApprecFellesformatDuplikat = Utils.readToFellesformat(
            "/apprec/legeerklaeringApprecDuplikat.xml")
    val expectedApprecFellesformatAvist = Utils.readToFellesformat(
            "/apprec/legeerklaeringApprecAvist.xml")
    val apprecStatusOK =  ApprecStatus.ok
    val apprecStatusAvvist =  ApprecStatus.avvist
    val apprecFellesformatOK = ApprecMapper().createApprec(inputMeldingFellesformat, apprecStatusOK, "")
    val apprecFellesformatDuplikat = ApprecMapper().createApprec(inputMeldingFellesformat, apprecStatusAvvist ,
            "DUPLICAT")
    val apprecFellesformatAvist = ApprecMapper().createApprec(inputMeldingFellesformat, apprecStatusAvvist ,
            "PATIENT_PERSON_NUMBER_NOT_VALID")
    val expectedCurrentDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar())

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkAvsender() {

        assertEquals(expectedApprecFellesformatOK.mottakenhetBlokk.avsender, apprecFellesformatOK.mottakenhetBlokk.avsender)
    }


    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEdiLoggId() {

        assertEquals(expectedApprecFellesformatOK.mottakenhetBlokk.ediLoggId, apprecFellesformatOK.mottakenhetBlokk.ediLoggId)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEbRole() {

        assertEquals(expectedApprecFellesformatOK.mottakenhetBlokk.ebRole, apprecFellesformatOK.mottakenhetBlokk.ebRole)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEbService() {

        assertEquals(expectedApprecFellesformatOK.mottakenhetBlokk.ebService, apprecFellesformatOK.mottakenhetBlokk.ebService)
    }

    @Test
    fun shouldCreateApprecOKWithMottakenhetBlokkEbAction() {

        assertEquals(expectedApprecFellesformatOK.mottakenhetBlokk.ebAction, apprecFellesformatOK.mottakenhetBlokk.ebAction)
    }

    @Test
    fun shouldCreateApprecOKWithMsgTypeDn() {

        assertEquals(expectedApprecFellesformatOK.appRec.msgType.dn, apprecFellesformatOK.appRec.msgType.dn)
    }

    @Test
    fun shouldCreateApprecOKWithMIGversion() {


        assertEquals(expectedApprecFellesformatOK.appRec.miGversion, apprecFellesformatOK.appRec.miGversion)
    }


    @Test
    fun shouldCreateApprecOKWithMGenDate() {

        assertEquals(expectedCurrentDate.day, apprecFellesformatOK.appRec.genDate.day)
        assertEquals(expectedCurrentDate.hour, apprecFellesformatOK.appRec.genDate.hour)
        assertEquals(expectedCurrentDate.minute, apprecFellesformatOK.appRec.genDate.minute)
    }

    @Test
    fun shouldCreateApprecOKWithApprecId() {

        assertEquals(expectedApprecFellesformatOK.appRec.id, apprecFellesformatOK.appRec.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstName() {

        assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.name, apprecFellesformatOK.appRec.sender.hcp.inst.name)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstId() {

        assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.id, apprecFellesformatOK.appRec.sender.hcp.inst.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstTypeIdDn() {

        assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.typeId.dn, apprecFellesformatOK.appRec.sender.hcp.inst.typeId.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstTypeIdV() {

        assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.typeId.v, apprecFellesformatOK.appRec.sender.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstId() {

         assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].id, apprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstTypeDn() {

        assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].type.dn, apprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].type.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstTypeV() {

        assertEquals(expectedApprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].type.v, apprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].type.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstName() {

        assertEquals(expectedApprecFellesformatOK.appRec.receiver.hcp.inst.name, apprecFellesformatOK.appRec.receiver.hcp.inst.name)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstId() {

        assertEquals(expectedApprecFellesformatOK.appRec.receiver.hcp.inst.id, apprecFellesformatOK.appRec.receiver.hcp.inst.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstTypeIdDn() {

        assertEquals(expectedApprecFellesformatOK.appRec.receiver.hcp.inst.typeId.dn, apprecFellesformatOK.appRec.receiver.hcp.inst.typeId.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstTypeIdVIs1() {

        assertEquals(expectedApprecFellesformatOK.appRec.receiver.hcp.inst.typeId.v, apprecFellesformatOK.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecOKWithStatusDnIsOK() {

        assertEquals(expectedApprecFellesformatOK.appRec.status.dn, apprecFellesformatOK.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithApprecReceiverHCPInstTypeIdVIs2() {

        assertEquals(expectedApprecFellesformatDuplikat.appRec.receiver.hcp.inst.typeId.v, apprecFellesformatDuplikat.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecDuplikatWithStatusDnIsAvist() {

        assertEquals(expectedApprecFellesformatDuplikat.appRec.status.dn, apprecFellesformatDuplikat.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErrosDn() {

        assertEquals(expectedApprecFellesformatDuplikat.appRec.error[0].dn, apprecFellesformatDuplikat.appRec.error[0].dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErrosV() {

        assertEquals(expectedApprecFellesformatDuplikat.appRec.error[0].v, apprecFellesformatDuplikat.appRec.error[0].v)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErross() {

        assertEquals(expectedApprecFellesformatDuplikat.appRec.error[0].s, apprecFellesformatDuplikat.appRec.error[0].s)
    }

    @Test
    fun shouldCreateApprecAvistWithApprecReceiverHCPInstTypeIdVIs2() {

        assertEquals(expectedApprecFellesformatAvist.appRec.receiver.hcp.inst.typeId.v, apprecFellesformatAvist.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecDAvistWithStatusDnIsAvist() {

        assertEquals(expectedApprecFellesformatAvist.appRec.status.dn, apprecFellesformatAvist.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecRAvistWithErroDn() {

        assertEquals(expectedApprecFellesformatAvist.appRec.error[0].dn, apprecFellesformatAvist.appRec.error[0].dn)
    }

    @Test
    fun shouldCreateApprecAvistWithErroV() {

        assertEquals(expectedApprecFellesformatAvist.appRec.error[0].v, apprecFellesformatAvist.appRec.error[0].v)
    }

    @Test
    fun shouldCreateApprecAvistWithErroS() {

        assertEquals(expectedApprecFellesformatAvist.appRec.error[0].s, apprecFellesformatAvist.appRec.error[0].s)
    }

    @Test
    fun shouldCreateApprecOKWithStatusV() {

        assertEquals(expectedApprecFellesformatOK.appRec.status.v, apprecFellesformatOK.appRec.status.v)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdMsgTypeDn() {

        assertEquals(expectedApprecFellesformatOK.appRec.originalMsgId.msgType.dn, apprecFellesformatOK.appRec.originalMsgId.msgType.dn)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdMsgTypeV() {

        assertEquals(expectedApprecFellesformatOK.appRec.originalMsgId.msgType.v, apprecFellesformatOK.appRec.originalMsgId.msgType.v)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdIssueDate() {

        assertEquals(expectedApprecFellesformatOK.appRec.originalMsgId.issueDate, apprecFellesformatOK.appRec.originalMsgId.issueDate)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdId() {

        assertEquals(expectedApprecFellesformatOK.appRec.originalMsgId.id, apprecFellesformatOK.appRec.originalMsgId.id)
    }
}