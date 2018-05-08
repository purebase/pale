package no.nav.legeerklaering.mapping

import no.nav.legeerklaering.LegeerklaeringConstant
import no.nav.legeerklaering.newInstance
import no.nav.model.apprec.*
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.msghead.Ident
import java.util.*

class ApprecMapper{
    fun createApprec(fellesformat: EIFellesformat, apprecStatus: ApprecStatus): EIFellesformat {

        val fellesformatApprec = EIFellesformat().apply {
            mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
                ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
                ebRole = LegeerklaeringConstant.ebRoleNav.string
                ebService = LegeerklaeringConstant.ebServiceLegemelding.string
                ebAction = LegeerklaeringConstant.ebActionSvarmelding.string
            }
            appRec = AppRec().apply {
                msgType = AppRecCS().apply {
                    v = LegeerklaeringConstant.APPREC.string
                }
                miGversion = LegeerklaeringConstant.APPRECVersionV1_0.string
                genDate = newInstance.newXMLGregorianCalendar(GregorianCalendar())
                id = fellesformat.mottakenhetBlokk.ediLoggId


                sender = AppRec.Sender().apply {
                    hcp = HCP().apply {
                        inst = Inst().apply {
                            name = fellesformat.msgHead.msgInfo.receiver.organisation.organisationName

                            for (i in fellesformat.msgHead.msgInfo.receiver.organisation.ident.indices) {
                                id = mapIdentToInst(fellesformat.msgHead.msgInfo.receiver.organisation.ident.first()).id
                                typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.receiver.organisation.ident.first()).typeId

                                val additionalIds = fellesformat.msgHead.msgInfo.receiver.organisation.ident.drop(1)
                                        .map { mapIdentToAdditionalId(it) }

                                additionalId.addAll(additionalIds)
                            }
                        }
                    }
                }

                receiver = AppRec.Receiver().apply {
                    hcp = HCP().apply {
                        inst = Inst().apply {
                            name = fellesformat.msgHead.msgInfo.sender.organisation.organisationName

                            for (i in fellesformat.msgHead.msgInfo.sender.organisation.ident.indices) {
                                id = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.ident.first()).id
                                typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.ident.first()).typeId

                                val additionalIds = fellesformat.msgHead.msgInfo.sender.organisation.ident.drop(1)
                                        .map { mapIdentToAdditionalId(it) }

                                additionalId.addAll(additionalIds)
                            }

                            hcPerson.add(HCPerson().apply {
                                name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName + " " +
                                        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName + " " +
                                        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.middleName

                                for (i in fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.indices) {
                                    id = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first()).id
                                    typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first()).typeId

                                    val additionalIds = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.drop(1)
                                            .map { mapIdentToAdditionalId(it) }

                                    additionalId.addAll(additionalIds)
                                }
                            }
                            )
                        }
                    }

                }

                status = AppRecCS().apply {
                    v = apprecStatus.v
                    dn = apprecStatus.dn
                }

                originalMsgId = OriginalMsgId().apply {
                    msgType = AppRecCS().apply {
                        v = LegeerklaeringConstant.LE.string
                        dn = LegeerklaeringConstant.Legeerklæring.string
                    }
                    issueDate = fellesformat.msgHead.msgInfo.genDate
                    id = fellesformat.msgHead.msgInfo.msgId
                }
            }
        }

        return fellesformatApprec
    }


    fun mapApprecErrorToAppRecCV(apprecError: ApprecError): AppRecCV = AppRecCV().apply {
        dn = apprecError.dn
        v = apprecError.v
        s = apprecError.s
    }

    fun mapIdentToAdditionalId(ident: Ident): AdditionalId = AdditionalId().apply {
        id = ident.id
        type = AppRecCS().apply {
            dn = ident.typeId.dn
            v = ident.typeId.v
        }
    }

    fun mapIdentToInst(ident: Ident): Inst = Inst().apply {
        id = ident.id
        typeId = AppRecCS().apply {
            dn = ident.typeId.dn
            v = ident.typeId.v
        }
    }

}
