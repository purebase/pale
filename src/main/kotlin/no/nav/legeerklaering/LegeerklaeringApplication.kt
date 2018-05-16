package no.nav.legeerklaering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import no.nav.legeerklaering.mapping.ApprecError
import no.nav.legeerklaering.mapping.ApprecStatus
import no.nav.legeerklaering.avro.DuplicateCheckedFellesformat
import no.nav.legeerklaering.mapping.createApprec
import no.nav.legeerklaering.mapping.mapApprecErrorToAppRecCV
import no.nav.legeerklaering.metrics.WS_CALL_TIME
import no.nav.legeerklaering.validation.*
import no.nav.model.fellesformat.*
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.fellesregistre.tssws_organisasjon.v3.TsswsOrganisasjonPortType
import no.nav.tjeneste.fellesregistre.tssws_organisasjon.v3.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Geografi
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.FinnNAVKontorRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
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



val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
val fellesformatJaxBContext = JAXBContext.newInstance(EIFellesformat::class.java, Legeerklaring::class.java)
val fellesformatUnmarshaller = fellesformatJaxBContext.createUnmarshaller()
val newInstance = DatatypeFactory.newInstance()
private val log = LoggerFactory.getLogger(LegeerklaeringApplication::class.java)

class LegeerklaeringApplication

fun main(args: Array<String>) {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()

    val connectionFactory = connectionFactory(fasitProperties)

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
                address = fasitProperties.virksomhetPersonV3EndpointURL
                features.add(LoggingFeature())
                serviceClass = PersonV3::class.java
            }.create() as PersonV3

            val tssOrganisasjon = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.tssWSOrganisasjonV4EndpointURL
                features.add(LoggingFeature())
                serviceClass = TsswsOrganisasjonPortType::class.java
            } as TsswsOrganisasjonPortType
            val orgnaisasjonEnhet = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.organisasjonEnhetV2EndpointURL
                features.add(LoggingFeature())
                serviceClass = OrganisasjonEnhetV2::class.java
            } as OrganisasjonEnhetV2

            val consumer = session.createConsumer(inputQueue)
            listen(consumer, jedis, personV3, tssOrganisasjon, orgnaisasjonEnhet)
        }
    }

}

fun listen(consumer: MessageConsumer, jedis: Jedis, personV3: PersonV3, tssOrganisasjon: TsswsOrganisasjonPortType, organisasjonEnhet: OrganisasjonEnhetV2) = consumer.setMessageListener {
    if (it is BytesMessage) {
        val bytes = ByteArray(it.bodyLength.toInt())
        it.readBytes(bytes)
        val fellesformat = fellesformatJaxBContext.createUnmarshaller().unmarshal(ByteArrayInputStream(bytes)) as EIFellesformat
        val legeerklaering = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

        val hashValue = createSha256Hash(objectMapper.writeValueAsBytes(legeerklaering))
        if(jedis.exists(hashValue)) {
            val apprec = createApprec(fellesformat, ApprecStatus.avvist)
            apprec.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.DUPLICAT))
            val outcome = OutcomeType.DUPLICATE
        } else {
            jedis.set(hashValue, fellesformat.mottakenhetBlokk.ediLoggId.toString())
        }

        val duplicateCheckedFellesformat = DuplicateCheckedFellesformat().apply {
            this.fellesformat = String(bytes, Charsets.UTF_8)
            this.retryCounter = 0
        }

        val outcomes = validateMessage(fellesformat, legeerklaering, personV3, organisasjonEnhet)
        val organisation = tssOrganisasjon.hentOrganisasjon(HentOrganisasjonRequest().apply {
            orgnummer = extractSenderOrganisationNumber(fellesformat)
        }).organisasjon
    }
}

fun validateMessage(fellesformat: EIFellesformat, legeerklaering: Legeerklaring, personV3: PersonV3, orgnaisasjonEnhet: OrganisasjonEnhetV2): List<Outcome> {
    val outcomes = mutableListOf<Outcome>()

    outcomes.addAll(validationFlow(fellesformat))

    outcomes.addAll(preTPSFlow(fellesformat))

    val personDeferred = personV3.hentPersonAsync(legeerklaering.pasientopplysninger.pasient.fodselsnummer)
    val geografiskTilknytningDeferred = personV3.hentGeografiskTilknytningAsync(legeerklaering.pasientopplysninger.pasient.fodselsnummer)

    val navKontorDeferred = async {
        WS_CALL_TIME.labels("finn_nav_kontor").startTimer().use {
            orgnaisasjonEnhet.finnNAVKontor(FinnNAVKontorRequest().apply {
                this.geografiskTilknytning = Geografi().apply {
                    this.value = geografiskTilknytningDeferred.await().geografiskTilknytning
                }
            }).navKontor
        }
    }

    val person = try {
        runBlocking { personDeferred.await() }
    } catch (e: HentPersonPersonIkkeFunnet) {
        outcomes += OutcomeType.PATIENT_NOT_FOUND_TPS
        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER)
        return outcomes
    } catch (e: HentPersonSikkerhetsbegrensning) {
        outcomes += when (e.faultInfo.sikkerhetsbegrensning[0].value) {
            "FP1_SFA" -> OutcomeType.PATIENT_HAS_SPERREKODE_6
            "FP2_FA" -> OutcomeType.PATIENT_HAS_SPERREKODE_7
            else -> throw RuntimeException("Missing handling of FP3_EA/Egen ansatt")
        }
        return outcomes
    }

    if (outcomes.none { it.outcomeType.messagePriority == Priority.RETUR }) {
        outcomes.addAll(postTPSFlow(fellesformat, person))
    }

    if (outcomes.none { it.outcomeType.messagePriority == Priority.RETUR }) {
        outcomes.addAll(postNORG2Flow(fellesformat, runBlocking { navKontorDeferred.await() }))
    }

    return outcomes
}

fun PersonV3.hentPersonAsync(fnr: String): Deferred<Person> = async {
    WS_CALL_TIME.labels("hent_person").startTimer().use {
        hentPerson(HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(
                        NorskIdent()
                                .withIdent(fnr)
                                .withType(Personidenter().withValue("FNR")))
                ).withInformasjonsbehov(Informasjonsbehov.FAMILIERELASJONER)).person
    }
}

fun PersonV3.hentGeografiskTilknytningAsync(fnr: String): Deferred<GeografiskTilknytning> = async {
    WS_CALL_TIME.labels("hent_geografisk_tilknytning").startTimer().use {
        hentGeografiskTilknytning(HentGeografiskTilknytningRequest().withAktoer(PersonIdent().withIdent(
                NorskIdent()
                        .withIdent(fnr)
                        .withType(Personidenter().withValue("FNR"))))).geografiskTilknytning
    }
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
