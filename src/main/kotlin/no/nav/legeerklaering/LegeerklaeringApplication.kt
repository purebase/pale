package no.nav.legeerklaering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import no.nav.model.fellesformat.AppRec
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.person.v3.HentPerson
import javax.jms.BytesMessage
import javax.jms.MessageConsumer
import javax.xml.datatype.XMLGregorianCalendar

val jaxbAnnotationModule = JaxbAnnotationModule()
val objectMapper: ObjectMapper = ObjectMapper().registerModule(jaxbAnnotationModule)

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
        val legeerklaering = objectMapper.readValue<Legeerklaring>(bytes, Legeerklaring::class.java)
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

fun createApprec(legeeklaering: Legeerklaring, gendate: XMLGregorianCalendar): String {

      val apprec = AppRec().apply {
          msgType.v = "APPREC"
          miGversion = "1.0 2004-11-21"
          genDate = gendate
      }


    return "lool"
}
