package no.nav.pale

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.prometheus.client.Summary
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.client.PdfClient
import no.nav.pale.client.PdfType
import no.nav.pale.client.Samhandler
import no.nav.pale.client.SamhandlerPraksis
import no.nav.pale.client.SarClient
import no.nav.pale.client.createArenaInfo
import no.nav.pale.client.createJoarkRequest
import no.nav.pale.mapping.ApprecError
import no.nav.pale.mapping.ApprecStatus
import no.nav.pale.mapping.createApprec
import no.nav.pale.mapping.mapApprecErrorToAppRecCV
import no.nav.pale.mapping.mapFellesformatToBehandlingsVedlegg
import no.nav.pale.mapping.mapFellesformatToFagmelding
import no.nav.pale.metrics.APPREC_ERROR_COUNTER
import no.nav.pale.metrics.APPREC_STATUS_COUNTER
import no.nav.pale.metrics.INCOMING_MESSAGE_COUNTER
import no.nav.pale.metrics.MESSAGE_OUTCOME_COUNTER
import no.nav.pale.metrics.QueueStatusCollector
import no.nav.pale.metrics.REQUEST_TIME
import no.nav.pale.metrics.RETRY_COUNTER
import no.nav.pale.metrics.WS_CALL_TIME
import no.nav.pale.sts.configureSTSFor
import no.nav.pale.validation.Outcome
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.Priority
import no.nav.pale.validation.extractDoctorIdentFromSignature
import no.nav.pale.validation.extractLegeerklaering
import no.nav.pale.validation.extractOrganisationNumberFromSender
import no.nav.pale.validation.extractPersonIdent
import no.nav.pale.validation.extractSenderOrganisationName
import no.nav.pale.validation.isDNR
import no.nav.pale.validation.postNORG2Flow
import no.nav.pale.validation.postSARFlow
import no.nav.pale.validation.postTPSFlow
import no.nav.pale.validation.preTPSFlow
import no.nav.pale.validation.shouldReturnEarly
import no.nav.pale.validation.toOutcome
import no.nav.pale.validation.toSystemSvar
import no.nav.pale.validation.validationFlow
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Geografi
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.FinnNAVKontorRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import redis.clients.jedis.exceptions.JedisConnectionException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Queue
import javax.jms.Session
import javax.jms.TextMessage
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.datatype.DatatypeFactory
import javax.xml.ws.WebServiceException
import kotlin.math.max

val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val redisMasterName = "mymaster"

val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(EIFellesformat::class.java, Legeerklaring::class.java)
val arenaEiaInfoJaxBContext: JAXBContext = JAXBContext.newInstance(ArenaEiaInfo::class.java)
val apprecJaxBContext: JAXBContext = JAXBContext.newInstance(EIFellesformat::class.java, AppRec::class.java)

val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()
val arenaMarshaller: Marshaller = arenaEiaInfoJaxBContext.createMarshaller()
val apprecMarshaller: Marshaller = apprecJaxBContext.createMarshaller()
val newInstance: DatatypeFactory = DatatypeFactory.newInstance()
val retryInterval = arrayOf(1000L, 1000L * 60, 2000L * 60, 2000L * 60, 5000L * 60)

private val log = LoggerFactory.getLogger("nav.pale-application")

class PaleApplication

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()

    createHttpServer(applicationVersion = fasitProperties.appVersion)

    connectionFactory(fasitProperties).createConnection(fasitProperties.mqUsername, fasitProperties.mqPassword).use {
        connection ->
        connection.start()
        JedisSentinelPool(redisMasterName, setOf("${fasitProperties.redisHost}:26379")).resource.use {
            jedis ->
            val session = connection.createSession()
            val inputQueue = session.createQueue(fasitProperties.inputQueueName)
            val arenaQueue = session.createQueue(fasitProperties.arenaQueueName)
            val receiptQueue = session.createQueue(fasitProperties.receiptQueueName)
            val backoutQueue = session.createQueue(fasitProperties.paleBackoutQueueName)
            session.close()

            val personV3 = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.personV3EndpointURL
                features.add(LoggingFeature())
                serviceClass = PersonV3::class.java
            }.create() as PersonV3
            configureSTSFor(personV3, fasitProperties.srvPaleUsername,
                    fasitProperties.srvPalePassword, fasitProperties.securityTokenServiceUrl)

            val orgnaisasjonEnhet = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.organisasjonEnhetV2EndpointURL
                features.add(LoggingFeature())
                serviceClass = OrganisasjonEnhetV2::class.java
            }.create() as OrganisasjonEnhetV2
            configureSTSFor(orgnaisasjonEnhet, fasitProperties.srvPaleUsername,
                    fasitProperties.srvPalePassword, fasitProperties.securityTokenServiceUrl)

            val interceptorProperties = mapOf(
                    WSHandlerConstants.USER to fasitProperties.srvPaleUsername,
                    WSHandlerConstants.ACTION to WSHandlerConstants.USERNAME_TOKEN,
                    WSHandlerConstants.PASSWORD_TYPE to WSConstants.PW_TEXT,
                    WSHandlerConstants.PW_CALLBACK_REF to CallbackHandler {
                        (it[0] as WSPasswordCallback).password = fasitProperties.srvPalePassword
                    }
            )

            val journalbehandling = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.journalbehandlingEndpointURL
                features.add(LoggingFeature())
                outInterceptors.add(WSS4JOutInterceptor(interceptorProperties))
                serviceClass = Journalbehandling::class.java
            }.create() as Journalbehandling

            val sarClient = SarClient(fasitProperties.kuhrSarApiURL, fasitProperties.srvPaleUsername,
                    fasitProperties.srvPalePassword)

            listen(PdfClient(fasitProperties.pdfGeneratorURL), jedis, personV3, orgnaisasjonEnhet,
                    journalbehandling, sarClient, inputQueue, arenaQueue, receiptQueue, backoutQueue, connection)
                    .join()
        }
    }
}

fun defaultLogInfo(keyValues: Array<StructuredArgument>): String =
        (0..(keyValues.size - 1)).joinToString(", ", "(", ")") { "{}" }

fun listen(
    pdfClient: PdfClient,
    jedis: Jedis,
    personV3: PersonV3,
    organisasjonEnhet: OrganisasjonEnhetV2,
    journalbehandling: Journalbehandling,
    sarClient: SarClient,
    inputQueue: Queue,
    arenaQueue: Queue,
    receiptQueue: Queue,
    backoutQueue: Queue,
    connection: Connection
) = launch {

    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val consumer = session.createConsumer(inputQueue)
    val arenaProducer = session.createProducer(arenaQueue)
    val receiptProducer = session.createProducer(receiptQueue)
    val backoutProducer = session.createProducer(backoutQueue)

    // Excluded arenaQueue due to no read rights
    QueueStatusCollector(connection, inputQueue, receiptQueue, backoutQueue).register<QueueStatusCollector>()

    consumer.setMessageListener {
        var defaultKeyValues = arrayOf(keyValue("noMessageIdentifier", true))
        var defaultKeyFormat = defaultLogInfo(defaultKeyValues)
        try {
            val inputMessageText = when (it) {
                is TextMessage -> it.text
                else -> throw RuntimeException("Incoming message needs to be a byte message or text message")
            }

            val fellesformat = fellesformatUnmarshaller.unmarshal(StringReader(inputMessageText)) as EIFellesformat

            val ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
            val sha256String = sha256hashstring(extractLegeerklaering(fellesformat))

            // TODO: Do we want to use markers for this instead?
            defaultKeyValues = arrayOf(
                    keyValue("organisationNumber", extractOrganisationNumberFromSender(fellesformat)?.id),
                    keyValue("ediLoggId", fellesformat.mottakenhetBlokk.ediLoggId),
                    keyValue("msgId", fellesformat.msgHead.msgInfo.msgId),
                    keyValue("messageId", sha256String)
            )

            defaultKeyFormat = defaultLogInfo(defaultKeyValues)

            log.info("Received message from {}, $defaultKeyFormat",
                    keyValue("size", inputMessageText.length),
                    *defaultKeyValues)

            if (log.isDebugEnabled) {
                log.debug("Incoming message {}, $defaultKeyFormat",
                        keyValue("xmlMessage", inputMessageText),
                        *defaultKeyValues)
            }

            try {
                val redisEdiLoggId = jedis.get(sha256String)
                val duplicate = redisEdiLoggId != null

                if (duplicate) {
                    sendReceipt(session, receiptProducer, fellesformat, ApprecStatus.avvist, ApprecError.DUPLICATE)
                    log.warn("Message marked as duplicate $defaultKeyFormat", redisEdiLoggId, *defaultKeyValues)
                    return@setMessageListener
                } else if (ediLoggId != null) {
                    jedis.setex(sha256String, TimeUnit.DAYS.toSeconds(7).toInt(), ediLoggId)
                }
            } catch (connectionException: JedisConnectionException) {
                log.warn("Unable to contact redis, will allow possible duplicates.", connectionException)
            }

            INCOMING_MESSAGE_COUNTER.inc()
            val requestLatency = REQUEST_TIME.startTimer()
            handleMessage(fellesformat, pdfClient, personV3, organisasjonEnhet, journalbehandling, sarClient, session,
                    arenaProducer, receiptProducer, defaultKeyFormat, defaultKeyValues, requestLatency)
        } catch (e: Exception) {
            log.error("Exception caught while handling message, sending to backout $defaultKeyFormat",
                    *defaultKeyValues, e)
            backoutProducer.send(it)
        }
    }

    while (isActive) {
        delay(100)
    }
}

fun handleMessage(
    fellesformat: EIFellesformat,
    pdfClient: PdfClient,
    personV3: PersonV3,
    organisasjonEnhet: OrganisasjonEnhetV2,
    journalbehandling: Journalbehandling,
    sarClient: SarClient,
    session: Session,
    arenaProducer: MessageProducer,
    receiptProducer: MessageProducer,
    defaultKeyFormat: String,
    defaultKeyValues: Array<StructuredArgument>,
    requestLatency: Summary.Timer
) {
    val validationResult = try {
        validateMessage(fellesformat, personV3, organisasjonEnhet, sarClient)
    } catch (e: Exception) {
        log.error("Exception caught while validating message, $defaultKeyFormat", *defaultKeyValues, e)
        throw e
    }

    log.debug("Outcomes: " + validationResult.outcomes.joinToString(", ", prefix = "\"", postfix = "\""))

    if (validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.RETUR }) {
        sendReceipt(session, receiptProducer, fellesformat, ApprecStatus.avvist, *validationResult.outcomes
                .mapNotNull { it.apprecError }
                .toTypedArray())
        val currentRequestLatency = requestLatency.observeDuration()
        log.info("Message $defaultKeyFormat has been sent in return, processing took {}s",
                *defaultKeyValues, currentRequestLatency)
        return
    }

    val manualHandling = validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.MANUAL_PROCESSING }

    when (manualHandling) {
        true -> MESSAGE_OUTCOME_COUNTER.labels(PaleConstant.eiaMan.string).inc()
        false -> MESSAGE_OUTCOME_COUNTER.labels(PaleConstant.eiaOk.string).inc()
    }

    val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFellesformatToFagmelding(fellesformat))
    val behandlingsVedlegg = mapFellesformatToBehandlingsVedlegg(fellesformat, validationResult.outcomes)
    val behandlingsvedleggPdf = pdfClient.generatePDF(PdfType.BEHANDLINGSVEDLEGG, behandlingsVedlegg)
    val joarkRequest = createJoarkRequest(fellesformat, fagmelding, behandlingsvedleggPdf, manualHandling)
    journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

    // Sperrekode 6 is a special case and is not sent to Arena, it should still create a task in Gosys
    if (validationResult.outcomes.any { it.outcomeType == OutcomeType.PATIENT_HAS_SPERREKODE_6 }) {
        log.info("Not sending message to arena $defaultKeyFormat", *defaultKeyValues)
    } else {
        log.info("Sending message to arena $defaultKeyFormat {}", *defaultKeyValues,
                keyValue("manualHandling", manualHandling))
        sendArenaInfo(arenaProducer, session, fellesformat, validationResult)
    }

    log.info("Sending apprec for $defaultKeyFormat", *defaultKeyValues)
    sendReceipt(session, receiptProducer, fellesformat, ApprecStatus.ok)

    val currentRequestLatency = requestLatency.observeDuration()
    log.info("Message $defaultKeyFormat was processed in {} s", *defaultKeyValues, currentRequestLatency)
}

fun sendArenaInfo(
    producer: MessageProducer,
    session: Session,
    fellesformat: EIFellesformat,
    validationResult: ValidationResult
) = producer.send(session.createTextMessage().apply {
    val sperrekode = when {
        validationResult.outcomes.any { it.outcomeType == OutcomeType.PATIENT_HAS_SPERREKODE_6 } -> 6
        validationResult.outcomes.any { it.outcomeType == OutcomeType.PATIENT_HAS_SPERREKODE_7 } -> 7
        else -> null
    }
    val info = createArenaInfo(fellesformat, validationResult.tssId, sperrekode, validationResult.navkontor).apply {
        // TODO eiaData may not be used by Arena
        eiaData = ArenaEiaInfo.EiaData().apply {
            systemSvar.addAll(validationResult.outcomes
                    .map { it.toSystemSvar() })

            if (systemSvar.isEmpty()) {
                systemSvar.add(OutcomeType.LEGEERKLAERING_MOTTAT.toOutcome().toSystemSvar())
            }
        }
    }
    text = arenaMarshaller.toString(info)
})

fun sendReceipt(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: EIFellesformat,
    apprecStatus: ApprecStatus,
    vararg apprecErrors: ApprecError
) {

    receiptProducer.send(session.createTextMessage().apply {
        val apprec = createApprec(fellesformat, apprecStatus)
        apprec.appRec.error.addAll(apprecErrors.map { mapApprecErrorToAppRecCV(it) })
        text = apprecMarshaller.toString(apprec)
        APPREC_STATUS_COUNTER.labels(apprecStatus.dn).inc()
        for (error in apprec.appRec.error) {
            APPREC_ERROR_COUNTER.labels(error.dn).inc()
        }
    })
}

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

data class ValidationResult(
    val tssId: String?,
    val outcomes: List<Outcome>,
    val navkontor: String?
)

fun List<Outcome>.toResult(
    tssId: String? = null,
    navkontor: String? = null
) = ValidationResult(tssId, this, navkontor)

fun validateMessage(
    fellesformat: EIFellesformat,
    personV3: PersonV3,
    orgnaisasjonEnhet: OrganisasjonEnhetV2,
    sarClient: SarClient
): ValidationResult {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val outcomes = mutableListOf<Outcome>()

    outcomes.addAll(validationFlow(fellesformat))
    if (outcomes.any { it.outcomeType.shouldReturnEarly() })
        return outcomes.toResult()

    outcomes.addAll(preTPSFlow(fellesformat))
    if (outcomes.any { it.outcomeType.shouldReturnEarly() })
        return outcomes.toResult()

    val patientIdent = extractPersonIdent(legeerklaering)!!
    val patientIdentType = when (isDNR(patientIdent)) {
        true -> PaleConstant.DNR.string
        false -> PaleConstant.FNR.string
    }

    val personDeferred = retryWithInterval(retryInterval, "hent_person") {
        val response = personV3.hentPerson(HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(
                        NorskIdent()
                                .withIdent(extractPersonIdent(legeerklaering)!!)
                                .withType(Personidenter().withValue(patientIdentType)))
                ).withInformasjonsbehov(Informasjonsbehov.FAMILIERELASJONER))
        response.person
    }

    val person = try {
        runBlocking {
            personDeferred.await()
        }
    } catch (e: HentPersonPersonIkkeFunnet) {
        outcomes += OutcomeType.PATIENT_NOT_FOUND_TPS.toOutcome(
                apprecError = ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER)
        return outcomes.toResult()
    }

    outcomes.addAll(postTPSFlow(fellesformat, person))
    if (outcomes.any { it.outcomeType.shouldReturnEarly() }) {
        return outcomes.toResult()
    }

    val geografiskTilknytningDeferred = retryWithInterval(retryInterval, "hent_geografisk_tilknytting") {
        personV3.hentGeografiskTilknytning(HentGeografiskTilknytningRequest().withAktoer(PersonIdent().withIdent(
                NorskIdent()
                        .withIdent(patientIdent)
                        .withType(Personidenter().withValue(patientIdent))))).geografiskTilknytning
    }

    val samhandlerDeferred = retryWithInterval(retryInterval, "kuhr_sar_hent_samhandler") {
        sarClient.getSamhandler(extractDoctorIdentFromSignature(fellesformat))
    }

    val navKontorDeferred = retryWithInterval(retryInterval, "finn_nav_kontor") {
        orgnaisasjonEnhet.finnNAVKontor(FinnNAVKontorRequest().apply {
            this.geografiskTilknytning = Geografi().apply {
                this.value = geografiskTilknytningDeferred.await()?.geografiskTilknytning ?: "0"
            }
        }).navKontor
    }

    outcomes.addAll(postNORG2Flow(runBlocking { navKontorDeferred.await() }))
    if (outcomes.any { it.outcomeType.shouldReturnEarly() }) {
        return outcomes.toResult()
    }
    val samhandler = runBlocking { samhandlerDeferred.await() }

    outcomes.addAll(postSARFlow(fellesformat, samhandler))

    return outcomes.toResult(findBestSamhandlerPraksis(samhandler, fellesformat)?.samhandlerPraksis?.tss_ident)
}

data class SamhandlerPraksisMatch(val samhandlerPraksis: SamhandlerPraksis, val percentageMatch: Double)

fun findBestSamhandlerPraksis(samhandlers: List<Samhandler>, fellesformat: EIFellesformat): SamhandlerPraksisMatch? {
    val orgName = extractSenderOrganisationName(fellesformat)
    val aktiveSamhandlere = samhandlers.flatMap { it.samh_praksis }
            .filter {
                it.samh_praksis_status_kode == "aktiv"
            }
            .filter {
                it.samh_praksis_periode
                        .filter { it.gyldig_fra <= DateTime.now() }
                        .filter { it.gyldig_til == null || it.gyldig_til >= DateTime.now() }
                        .any()
            }
            .filter { !it.navn.isNullOrEmpty() }
            .toList()

    return aktiveSamhandlere
            .map {
                SamhandlerPraksisMatch(it, calculatePercentageStringMatch(it.navn, orgName))
            }.sortedBy { it.percentageMatch }
            .firstOrNull()
}

fun calculatePercentageStringMatch(str1: String?, str2: String): Double {
    val maxDistance = max(str1?.length!!, str2.length).toDouble()
    val distance = LevenshteinDistance().apply(str2, str1).toDouble()
    return (maxDistance - distance) / maxDistance
}

fun <T> retryWithInterval(interval: Array<Long>, callName: String, blocking: suspend () -> T): Deferred<T> {
    return async {
        for (time in interval) {
            try {
                WS_CALL_TIME.labels(callName).startTimer().use {
                    return@async blocking()
                }
            } catch (e: WebServiceException) {
                if (e.cause !is IOException)
                    throw e
                log.warn("Caught IO exception trying to reach {}, retrying", callName, e)
            }
            RETRY_COUNTER.labels(callName).inc()
            Thread.sleep(time)
        }

        WS_CALL_TIME.labels(callName).startTimer().use {
            blocking()
        }
    }
}

fun connectionFactory(fasitProperties: FasitProperties) = MQConnectionFactory().apply {
    hostName = fasitProperties.mqHostname
    port = fasitProperties.mqPort
    queueManager = fasitProperties.mqQueueManagerName
    transportType = WMQConstants.WMQ_CM_CLIENT
    // TODO mq crypo
    // sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = fasitProperties.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun getHCPFodselsnummer(fellesformat: EIFellesformat): String? =
        fellesformat.msgHead.msgInfo.sender.organisation?.healthcareProfessional?.ident
                ?.find { it.typeId.v == "FNR" }?.id ?: ""

fun sha256hashstring(legeerklaering: Legeerklaring): String =
        MessageDigest.getInstance("SHA-256")
                .digest(objectMapper.writeValueAsBytes(legeerklaering))
                .fold("") { str, it -> str + "%02x".format(it) }
