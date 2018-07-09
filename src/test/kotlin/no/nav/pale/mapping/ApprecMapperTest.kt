package no.nav.pale.mapping

import no.nav.pale.PaleConstant
import no.nav.pale.utils.readToFellesformat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

class ApprecMapperTest {

    val inputMeldingFellesformat = readToFellesformat("/legeerklaering.xml")
    val apprecStatusOK = ApprecStatus.ok
    val apprecStatusAvvist = ApprecStatus.avvist
    val apprecFellesformatOK = createApprec(inputMeldingFellesformat, apprecStatusOK)
    val apprecFellesformatDuplikat = createApprec(inputMeldingFellesformat, apprecStatusAvvist)
    val apprecFellesformaBehanderlPersonNumberNotValid = createApprec(inputMeldingFellesformat, apprecStatusAvvist)
    val expectedCurrentDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar())

    @Before
    fun setup() {
        apprecFellesformatDuplikat.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.DUPLICATE))
        apprecFellesformaBehanderlPersonNumberNotValid.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID))
    }

    @Test
    fun shouldCreateApprecWithMottakenhetBlokkEdiLoggId() {

        assertEquals(inputMeldingFellesformat.mottakenhetBlokk.ediLoggId, apprecFellesformatOK.mottakenhetBlokk.ediLoggId)
    }

    @Test
    fun shouldCreateApprecWithMottakenhetBlokkEbRole() {

        assertEquals(PaleConstant.ebRoleNav.string, apprecFellesformatOK.mottakenhetBlokk.ebRole)
    }

    @Test
    fun shouldCreateApprecWithMottakenhetBlokkEbService() {

        assertEquals(PaleConstant.ebServiceLegemelding.string, apprecFellesformatOK.mottakenhetBlokk.ebService)
    }

    @Test
    fun shouldCreateApprecWithMottakenhetBlokkEbAction() {

        assertEquals(PaleConstant.ebActionSvarmelding.string, apprecFellesformatOK.mottakenhetBlokk.ebAction)
    }

    @Test
    fun shouldCreateApprecWithMsgTypeV() {

        assertEquals(PaleConstant.APPREC.string, apprecFellesformatOK.appRec.msgType.v)
    }

    @Test
    fun shouldCreateApprecWithMIGversion() {

        assertEquals(PaleConstant.APPRECVersionV1_0.string, apprecFellesformatOK.appRec.miGversion)
    }

    @Test
    fun shouldCreateApprecOKWithMGenDate() {

        assertEquals(expectedCurrentDate.day, apprecFellesformatOK.appRec.genDate.day)
        assertEquals(expectedCurrentDate.hour, apprecFellesformatOK.appRec.genDate.hour)
        assertEquals(expectedCurrentDate.minute, apprecFellesformatOK.appRec.genDate.minute)
    }

    @Test
    fun shouldCreateApprecOKWithApprecId() {

        assertEquals(inputMeldingFellesformat.mottakenhetBlokk.ediLoggId, apprecFellesformatOK.appRec.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstName() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.receiver.organisation.organisationName, apprecFellesformatOK.appRec.sender.hcp.inst.name)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstId() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.receiver.organisation.ident[0].id, apprecFellesformatOK.appRec.sender.hcp.inst.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstTypeIdDn() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.receiver.organisation.ident[0].typeId.dn, apprecFellesformatOK.appRec.sender.hcp.inst.typeId.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstTypeIdV() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.receiver.organisation.ident[0].typeId.v, apprecFellesformatOK.appRec.sender.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstId() {
        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.receiver.organisation.ident[1].id, apprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstTypeDn() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.receiver.organisation.ident[1].typeId.dn, apprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].type.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecSenderHCPInstAdditionalIdFirstTypeV() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.ident[0].typeId.v, apprecFellesformatOK.appRec.sender.hcp.inst.additionalId[0].type.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstName() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.organisationName, apprecFellesformatOK.appRec.receiver.hcp.inst.name)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstId() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.ident[0].id, apprecFellesformatOK.appRec.receiver.hcp.inst.id)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstTypeIdDn() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.ident[0].typeId.dn, apprecFellesformatOK.appRec.receiver.hcp.inst.typeId.dn)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstTypeIdVIs1() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.ident[0].typeId.v, apprecFellesformatOK.appRec.receiver.hcp.inst.typeId.v)
    }

    @Test
    fun shouldCreateApprecOKWithApprecReceiverHCPInstHCPersonName() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName + " " +
                inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName + " " +
                inputMeldingFellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName, apprecFellesformatOK.appRec.receiver.hcp.inst.hcPerson[0].name)
    }

    @Test
    fun shouldCreateApprecOKWithStatusDnIsOK() {

        assertEquals(apprecStatusOK.dn, apprecFellesformatOK.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithStatusDnIsAvist() {

        assertEquals(apprecStatusAvvist.dn, apprecFellesformatDuplikat.appRec.status.dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErrosDn() {

        assertEquals(ApprecError.DUPLICATE.dn, apprecFellesformatDuplikat.appRec.error[0].dn)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErrosV() {

        assertEquals(ApprecError.DUPLICATE.v, apprecFellesformatDuplikat.appRec.error[0].v)
    }

    @Test
    fun shouldCreateApprecDuplikatWithErross() {

        assertEquals(ApprecError.DUPLICATE.s, apprecFellesformatDuplikat.appRec.error[0].s)
    }

    @Test
    fun shouldCreateApprecRAvistWithErroDn() {

        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.dn, apprecFellesformaBehanderlPersonNumberNotValid.appRec.error[0].dn)
    }

    @Test
    fun shouldCreateApprecAvistWithErroV() {

        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v, apprecFellesformaBehanderlPersonNumberNotValid.appRec.error[0].v)
    }

    @Test
    fun shouldCreateApprecAvistWithErroS() {

        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.s, apprecFellesformaBehanderlPersonNumberNotValid.appRec.error[0].s)
    }

    @Test
    fun shouldCreateApprecOKWithStatusV() {

        assertEquals(ApprecStatus.ok.v, apprecFellesformatOK.appRec.status.v)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdMsgTypeDn() {

        assertEquals(PaleConstant.Legeerklæring.string, apprecFellesformatOK.appRec.originalMsgId.msgType.dn)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdMsgTypeV() {

        assertEquals(PaleConstant.LE.string, apprecFellesformatOK.appRec.originalMsgId.msgType.v)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdIssueDate() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.genDate, apprecFellesformatOK.appRec.originalMsgId.issueDate)
    }

    @Test
    fun shouldCreateApprecOKWithOriginalMsgIdId() {

        assertEquals(inputMeldingFellesformat.msgHead.msgInfo.msgId, apprecFellesformatOK.appRec.originalMsgId.id)
    }
}
