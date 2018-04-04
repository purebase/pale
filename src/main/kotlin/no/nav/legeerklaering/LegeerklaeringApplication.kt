package no.nav.legeerklaering

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import no.nav.model.fellesformat.*
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.person.v3.HentPerson
import java.util.*
import javax.jms.BytesMessage
import javax.jms.MessageConsumer
import javax.xml.datatype.DatatypeFactory


val jaxbAnnotationModule = JaxbAnnotationModule().apply {

}
val jacksonXmlModule = JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}
val objectMapper: ObjectMapper = XmlMapper(jacksonXmlModule).registerModule(jaxbAnnotationModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
val newInstance = DatatypeFactory.newInstance()

fun main(args: Array<String>) {
    val fasitProperties = FasitProperties()

    val connectionFactory = connectionFactory(fasitProperties)

    val connection = connectionFactory.createConnection()
    connection.start()

    connection.use {
        val session = it.createSession()
        val inputQueue = session.createQueue(fasitProperties.legeerklaeringQueueName)
        val outputQueue = session.createQueue(fasitProperties.arenaQueueName)
        val hentPerson = HentPerson()

        val consumer = session.createConsumer(inputQueue)
        listen(consumer)
    }

}

fun listen(consumer: MessageConsumer) = consumer.setMessageListener {
    if (it is BytesMessage) {
        val bytes = ByteArray(it.bodyLength.toInt())
        it.readBytes(bytes)
        val fellesformat = objectMapper.readValue<EIFellesformat>(bytes, EIFellesformat::class.java)
        val legeerklaering = fellesformat.msgHead.document.get(0).refDoc.content.any as Legeerklaring
        val outcome = validateMessage(legeerklaering)

        when (outcome) {
            is Success -> {
                // Send message to a internal kafka topic, then poll it and run the enrichment process
            }

            is ValidationError -> {
                // Inform sender that the message was invalid
            }
        }
    }
}

fun validateMessage(legeeklaering: Legeerklaring): Outcome {
    val pasient = legeeklaering.pasientopplysninger.pasient
    if (!validatePersonNumber(pasient.fodselsnummer)) {
        ValidationError("Invalid personnummer, checksum not matching")
    }

    return Success()
}

fun connectionFactory(fasitProperties: FasitProperties) = MQConnectionFactory().apply {
    hostName = fasitProperties.mqHostname
    port = fasitProperties.mqPort
    queueManager = fasitProperties.mqQueueManagerName
    transportType = WMQConstants.WMQ_CM_CLIENT
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun createApprec(fellesformat: EIFellesformat): EIFellesformat {

    val fellesformatApprec = EIFellesformat().apply {
        mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
            ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
            ebRole = LegeerklaeringConstant.ebRoleNav.string
            ebService = LegeerklaeringConstant.ebServiceLegemelding.string
            ebAction = LegeerklaeringConstant.ebActionSvarmelding.string
        }
        AppRec().apply {
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
                            name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName

                            for (i in fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.indices) {
                                id = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first()).id
                                typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first()).typeId

                                val additionalIds = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.drop(1)
                                        .map { mapIdentToAdditionalId(it) }

                                additionalId.addAll(additionalIds)
                            }
                        })

                    }
            }
                status = AppRecCS().apply {
                    v = "1"
                    dn = "OK"
                }
                originalMsgId = OriginalMsgId().apply {
                    msgType = AppRecCS().apply {
                        v = "LE"
                        dn = "Legeerklæring"
                    }
                    issueDate = fellesformat.msgHead.msgInfo.genDate
                    id = fellesformat.msgHead.msgInfo.msgId
                }
            }
        }
    }
    return fellesformatApprec
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
    typeId= AppRecCS().apply {
        dn = ident.typeId.dn
        v = ident.typeId.v
    }
}
