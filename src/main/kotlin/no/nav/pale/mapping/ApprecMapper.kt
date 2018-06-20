package no.nav.pale.mapping

import no.nav.pale.PaleConstant
import no.nav.pale.newInstance
import no.nav.model.apprec.*
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.msghead.HealthcareProfessional
import no.nav.model.msghead.Ident
import no.nav.model.msghead.MsgHeadCV
import no.nav.model.msghead.Organisation
import java.util.*

fun createApprec(fellesformat: EIFellesformat, apprecStatus: ApprecStatus): EIFellesformat {
    val fellesformatApprec = EIFellesformat().apply {
        mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
            ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
            ebRole = PaleConstant.ebRoleNav.string
            ebService = PaleConstant.ebServiceLegemelding.string
            ebAction = PaleConstant.ebActionSvarmelding.string
        }
        appRec = AppRec().apply {
            msgType = AppRecCS().apply {
                v = PaleConstant.APPREC.string
            }
            miGversion = PaleConstant.APPRECVersionV1_0.string
            genDate = newInstance.newXMLGregorianCalendar(GregorianCalendar())
            id = fellesformat.mottakenhetBlokk.ediLoggId

            sender = AppRec.Sender().apply {
                hcp = fellesformat.msgHead.msgInfo.receiver.organisation.intoHCP()
            }

            receiver = AppRec.Receiver().apply {
                hcp = fellesformat.msgHead.msgInfo.sender.organisation.intoHCP()
            }

            status = AppRecCS().apply {
                v = apprecStatus.v
                dn = apprecStatus.dn
            }

            originalMsgId = OriginalMsgId().apply {
                msgType = AppRecCS().apply {
                    v = PaleConstant.LE.string
                    dn = PaleConstant.Legeerklæring.string
                }
                issueDate = fellesformat.msgHead.msgInfo.genDate
                id = fellesformat.msgHead.msgInfo.msgId
            }
        }
    }

    return fellesformatApprec
}

fun HealthcareProfessional.intoHCPerson(): HCPerson = HCPerson().apply {
    if (middleName == null)
        middleName = ""
    name = "$familyName $givenName $middleName".trim()
    id = ident.first().id
    typeId = ident.first().typeId.intoAppRecCS()
    additionalId += ident.drop(1)
}

fun Organisation.intoHCP(): HCP = HCP().apply {
    inst = ident.first().intoInst().apply {
        name = organisationName
        additionalId += ident.drop(1)

        if (healthcareProfessional != null) {
            hcPerson += healthcareProfessional.intoHCPerson()
        }
    }
}

fun Ident.intoInst(): Inst {
    val ident = this
    return Inst().apply {
        id = ident.id
        typeId = ident.typeId.intoAppRecCS()
    }
}

fun MsgHeadCV.intoAppRecCS(): AppRecCS {
    val msgHeadCV = this
    return AppRecCS().apply {
        dn = msgHeadCV.dn
        v = msgHeadCV.v
    }
}

operator fun MutableList<AdditionalId>.plusAssign(idents: Iterable<Ident>) {
    this.addAll(idents.map { it.intoAdditionalId() })
}

fun Ident.intoAdditionalId(): AdditionalId {
    val ident = this
    return AdditionalId().apply {
        id = ident.id
        type = AppRecCS().apply {
            dn = ident.typeId.dn
            v = ident.typeId.v
        }
    }
}

fun mapApprecErrorToAppRecCV(apprecError: ApprecError): AppRecCV = AppRecCV().apply {
    dn = apprecError.dn
    v = apprecError.v
    s = apprecError.s
}
