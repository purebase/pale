package no.nav.legeerklaering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import no.nav.legeerklaering.apprec.mapper.ApprecMapper
import no.nav.legeerklaering.apprec.mapper.ApprecStatus
import no.nav.legeerklaering.avro.DuplicateCheckedFellesformat
import no.nav.legeerklaering.config.EnvironmentConfig
import no.nav.legeerklaering.validation.*
import no.nav.model.fellesformat.*
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.fellesregistre.tssws_organisasjon.v3.TsswsOrganisasjonPortType
import no.nav.tjeneste.fellesregistre.tssws_organisasjon.v3.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.slf4j.LoggerFactory
import javax.jms.BytesMessage
import javax.jms.MessageConsumer
import javax.xml.datatype.DatatypeFactory
import java.security.MessageDigest
import redis.clients.jedis.Jedis
import java.io.ByteArrayInputStream
import java.math.BigInteger
import javax.xml.bind.JAXBContext


val jaxbAnnotationModule = JaxbAnnotationModule()
val jacksonXmlModule = JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}
//val objectMapper: ObjectMapper = XmlMapper(jacksonXmlModule).registerModule(jaxbAnnotationModule)
//        .enable(SerializationFeature.INDENT_OUTPUT)
val objectMapper: ObjectMapper = ObjectMapper()
val fellesformatJaxBContext = JAXBContext.newInstance(EIFellesformat::class.java, Legeerklaring::class.java)
val fellesformatUnmarshaller = fellesformatJaxBContext.createUnmarshaller()
val newInstance = DatatypeFactory.newInstance()
private val log = LoggerFactory.getLogger(LegeerklaeringApplication::class.java)

class LegeerklaeringApplication {

    fun main(args: Array<String>) {
        val fasitProperties = FasitProperties()

        val connectionFactory = connectionFactory(fasitProperties)

        val environmentConfig = EnvironmentConfig()

        val connection = connectionFactory.createConnection()
        connection.start()

        connection.use {
            queueConnection ->

            Jedis().use {
                jedis ->
                val session = queueConnection.createSession()
                val inputQueue = session.createQueue(fasitProperties.legeerklaeringQueueName)
                val outputQueue = session.createQueue(fasitProperties.arenaQueueName)

                val personV3 = JaxWsProxyFactoryBean().apply {
                    address = environmentConfig.virksomhetPersonV3EndpointURL
                    features.add(LoggingFeature())
                    serviceClass = PersonV3::class.java
                }.create() as PersonV3

                val tssOrganisasjon = JaxWsProxyFactoryBean().apply {
                    address = environmentConfig.tssWSOrganisasjonV4EndpointURL
                    features.add(LoggingFeature())
                    serviceClass = TsswsOrganisasjonPortType::class.java
                } as TsswsOrganisasjonPortType


                val consumer = session.createConsumer(inputQueue)
                listen(consumer, jedis, personV3, tssOrganisasjon)
            }
        }

    }

    fun listen(consumer: MessageConsumer, jedis: Jedis, personV3: PersonV3, tssOrganisasjon: TsswsOrganisasjonPortType) = consumer.setMessageListener {
        if (it is BytesMessage) {
            val bytes = ByteArray(it.bodyLength.toInt())
            it.readBytes(bytes)
            val fellesformat = fellesformatJaxBContext.createUnmarshaller().unmarshal(ByteArrayInputStream(bytes)) as EIFellesformat
            val legeerklaering = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

            val hashValue = createSha256Hash(objectMapper.writeValueAsBytes(legeerklaering))
            if(jedis.exists(hashValue)) {
                ApprecMapper().createApprec(fellesformat, ApprecStatus.ok, "")
            } else {
                jedis.set(hashValue, fellesformat.mottakenhetBlokk.ediLoggId.toString())
            }

            val duplicateCheckedFellesformat = DuplicateCheckedFellesformat().apply {
                setFellesformat(String(bytes, Charsets.UTF_8))
                setRetryCounter(0)
            }

            val outcomes = validateMessage(fellesformat, legeerklaering, personV3)

            val organisation = tssOrganisasjon.hentOrganisasjon(HentOrganisasjonRequest().apply {
                orgnummer = extractSenderOrganisationNumber(fellesformat)
            }).organisasjon
        }
    }

    fun validateMessage(fellesformat: EIFellesformat, legeerklaering: Legeerklaring, personV3: PersonV3): List<OutcomeType> {
        val outcomes = mutableListOf<OutcomeType>()

        val person = try {
            personV3.hentPerson(HentPersonRequest()
                    .withAktoer(PersonIdent().withIdent(
                            NorskIdent()
                                    .withIdent(legeerklaering.pasientopplysninger.pasient.fodselsnummer)
                                    .withType(Personidenter().withValue("FNR")))
                    ).withInformasjonsbehov(Informasjonsbehov.FAMILIERELASJONER)).person
        } catch (e: HentPersonPersonIkkeFunnet) {
            outcomes.add(OutcomeType.PATIENT_NOT_FOUND_IN_TPS)
            return outcomes
        } catch (e: HentPersonSikkerhetsbegrensning) {
            outcomes.add(when (e.faultInfo.sikkerhetsbegrensning[0].value) {
                "FP1_SFA" -> OutcomeType.PATIENT_HAS_SPERREKODE_6
                "FP2_FA" -> OutcomeType.PATIENT_HAS_SPERREKODE_7
                else -> throw RuntimeException("Missing handling of FP3_EA/Egen ansatt")
            })
            return outcomes
        }

        outcomes.addAll(validationFlow(fellesformat))

        outcomes.addAll(preTSSFlow(fellesformat))

        if (outcomes.none { true }) {
            outcomes.addAll(postTSSFlow(fellesformat, person))
        }

        return outcomes
    }


    fun createSha256Hash(input: ByteArray): String {
        val bytes = MessageDigest
                .getInstance("SHA1")
                .digest(input)

        return BigInteger(bytes).toString(16)
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

    fun checkIfHashValueIsInRedis(jedis: Jedis,hashValue: String): Boolean =
            jedis.get(hashValue) != null


    fun getHCPFodselsnummer(fellesformat: EIFellesformat): String {

        for (ident in fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident) {

            if (ident.typeId.v.equals("FNR"))
            {
                return ident.id
            }

        }

        return ""

    }

}
