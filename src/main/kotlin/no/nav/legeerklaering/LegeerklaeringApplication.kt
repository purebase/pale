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
import org.slf4j.LoggerFactory
import java.util.*
import javax.jms.BytesMessage
import javax.jms.MessageConsumer
import javax.xml.datatype.DatatypeFactory
import java.security.MessageDigest
import redis.clients.jedis.Jedis


val jaxbAnnotationModule = JaxbAnnotationModule()
val jacksonXmlModule = JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}
val objectMapper: ObjectMapper = XmlMapper(jacksonXmlModule).registerModule(jaxbAnnotationModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
val newInstance = DatatypeFactory.newInstance()
private val log = LoggerFactory.getLogger(LegeerklaeringApplication::class.java!!)

class LegeerklaeringApplication {

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

            val hashValue = createSHA1(fellesformat.toString())

            val jedis = Jedis("localhost", 6379)
            log.debug("Connection to reids server sucessfully")
            log.debug("Redis server is running: " + jedis.ping())

            val duplicate = checkIfHashValueIsInRedis(jedis,hashValue)

            if(duplicate) {
                createApprec(fellesformat, "duplikat")
            }
            else
            {
                jedis.set(hashValue, fellesformat.mottakenhetBlokk.ediLoggId.toString())
            }

            val legeerklaering = fellesformat.msgHead.document.get(0).refDoc.content.any as Legeerklaring
            val outcome = validateMessage(legeerklaering)

            when (outcome) {
                is Success -> {
                    // Send message to a internal kafka topic, then poll it and run the enrichment process
                }

                is ValidationError -> {
                    createApprec(fellesformat, "avvist")
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

    fun createApprec(fellesformat: EIFellesformat, apprecStatus: String): EIFellesformat {

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
                                name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName

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

                when {
                    apprecStatus.equals("avvist") -> {
                        status = createApprecStatus(2)
                        error.add(AppRecCV().apply {
                            //@TODO change dn(Get the rule description thas denies the message) and the correct v
                            dn = "Pasienten sitt fødselsnummer eller D-nummer '010101' er ikke 11 tegn. Det er 6 tegn langt."
                            v = "47"
                            s = "2.16.578.1.12.4.1.1.8223"
                        })
                    }

                    apprecStatus.equals("duplikat") -> {
                        status = createApprecStatus(2)
                        error.add(AppRecCV().apply {
                            dn = "Duplikat! - Denne meldingen er mottatt tidligere. Skal ikke sendes på nytt."
                            v = "801"
                            s = "2.16.578.1.12.4.1.1.8223"
                        })
                    }


                    apprecStatus.equals("ok") ->
                        status = createApprecStatus(1)

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

    fun createApprecStatus(status: Int): AppRecCS = AppRecCS().apply {
        if (status == 2) {
            v = "2"
            dn = "Avvist"
        } else {
            v = "1"
            dn = "OK"
        }
    }


    fun createSHA1(input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance("sha1")
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }

    fun checkIfHashValueIsInRedis(jedis: Jedis,hashValue: String): Boolean {

        if (jedis.get(hashValue) == null) {
            log.info("hashValue is not in redis")
            return false
        } else {
            log.info("hashValue is  in redis")
            return true

        }
    }


}