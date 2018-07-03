package no.nav.pale

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
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.pale.sts.configureSTSFor
import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.Legeerklaring
import no.nav.pale.client.PdfClient
import no.nav.pale.client.PdfType
import no.nav.pale.client.Samhandler
import no.nav.pale.client.SamhandlerPraksis
import no.nav.pale.client.SarClient
import no.nav.pale.client.createArenaEiaInfo
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
import no.nav.pale.metrics.WS_CALL_TIME
import no.nav.pale.validation.Outcome
import no.nav.pale.validation.OutcomeType
import no.nav.pale.validation.Priority
import no.nav.pale.validation.extractOrganisationNumberFromSender
import no.nav.pale.validation.extractDoctorIdentFromSignature
import no.nav.pale.validation.extractLegeerklaering
import no.nav.pale.validation.extractPersonIdent
import no.nav.pale.validation.extractSenderOrganisationName
import no.nav.pale.validation.isDNR
import no.nav.pale.validation.postNORG2Flow
import no.nav.pale.validation.postSARFlow
import no.nav.pale.validation.postTPSFlow
import no.nav.pale.validation.preTPSFlow
import no.nav.pale.validation.shouldReturnEarly
import no.nav.pale.validation.toOutcome
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
import org.slf4j.LoggerFactory
import javax.xml.datatype.DatatypeFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.io.IOException
import java.io.StringWriter
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.jms.BytesMessage
import javax.jms.Connection
import javax.jms.Queue
import javax.jms.Session
import javax.jms.TextMessage
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.transform.stream.StreamSource
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
val arenaEiaInfoMarshaller: Marshaller = arenaEiaInfoJaxBContext.createMarshaller()
val apprecMarshaller: Marshaller = apprecJaxBContext.createMarshaller()
val newInstance: DatatypeFactory = DatatypeFactory.newInstance()
val retryInterval = arrayOf(1000L * 60, 2000L * 60, 2000L * 60, 5000L * 60)

private val log = LoggerFactory.getLogger("pale-application")

class PaleApplication

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()

    createHttpServer(applicationVersion = fasitProperties.appVersion)

    connectionFactory(fasitProperties).createConnection(fasitProperties.mqUsername, fasitProperties.mqPassword).use {
        connection ->
        connection.start()
        val sentinels = setOf("rfs-${fasitProperties.appName}:26379")
        JedisSentinelPool(redisMasterName, sentinels).resource.use {
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

    var defaultKeyValues = arrayOf(keyValue("noMessageIdentifier", true))
    var defaultKeyFormat = defaultLogInfo(defaultKeyValues)

    // Excluded arenaQueue due to no read rights
    QueueStatusCollector(connection, inputQueue, receiptQueue, backoutQueue)
            .register<QueueStatusCollector>()

    consumer.setMessageListener {
        try {
            val inputMessageText = when (it) {
                is BytesMessage -> {
                    val bytes = ByteArray(it.bodyLength.toInt())
                    it.readBytes(bytes)
                    String(bytes, Charsets.ISO_8859_1)
                }
                is TextMessage -> it.text
                else -> throw RuntimeException("Incoming message needs to be a byte message or text message")
            }

            val fellesformat = fellesformatUnmarshaller.unmarshal(StreamSource(inputMessageText.byteInputStream()), EIFellesformat::class.java).value
            INCOMING_MESSAGE_COUNTER.inc()
            val requestLatency = REQUEST_TIME.startTimer()

            var ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
            var sha256String = sha256hashstring(extractLegeerklaering(fellesformat))

            defaultKeyValues = arrayOf(
                    keyValue("organisationNumber",extractOrganisationNumberFromSender(fellesformat)?.id),
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

            val jedisSha256String = jedis.get(sha256String)
            val duplicate = jedisSha256String != null

            if (duplicate) {
                log.info("Sending Duplicate Avvist apprec for $defaultKeyFormat", *defaultKeyValues)
                receiptProducer.send(session.createTextMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                    apprec.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.DUPLICAT))
                    log.warn("Message with ediloggId {} marked as duplicate $defaultKeyFormat", jedisSha256String,
                            *defaultKeyValues)
                    text = apprecMarshaller.toString(apprec)
                    APPREC_ERROR_COUNTER.labels(ApprecError.DUPLICAT.dn).inc()
                    APPREC_STATUS_COUNTER.labels(ApprecStatus.avvist.dn).inc()
                })
                return@setMessageListener
            } else if (ediLoggId != null) {
                jedis.setex(sha256String, TimeUnit.DAYS.toSeconds(7).toInt(), ediLoggId)
            }

            val validationResult = try {
                validateMessage(fellesformat, personV3, organisasjonEnhet, sarClient)
            } catch (e: Exception) {
                log.error("Exception caught while validating message, $defaultKeyFormat", *defaultKeyValues, e)
                throw e
            }

            if (validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.RETUR }) {
                log.info("Sending Avvist apprec for $defaultKeyFormat", *defaultKeyValues)

                //TODO REMOVE AFTER TESTING STAGE
                var outcomesString =  ""
                validationResult.outcomes.forEach{ outcomesString += it.outcomeType.name + ", "}
                log.info("validationResult.outcomes {}, $defaultKeyFormat",
                        keyValue("outcomes" , outcomesString),
                        *defaultKeyValues)

                receiptProducer.send(session.createTextMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                    apprec.appRec.error.addAll(validationResult.outcomes
                            .filter { it.outcomeType.messagePriority == Priority.RETUR }
                            .map { mapApprecErrorToAppRecCV(it.apprecError!!) }
                    )
                    text = apprecMarshaller.toString(apprec)
                    receiptProducer.send(this)
                    for (error in apprec.appRec.error) {
                        APPREC_ERROR_COUNTER.labels(error.dn).inc()
                    }
                    APPREC_STATUS_COUNTER.labels(ApprecStatus.avvist.dn).inc()
                })
            } else {
                val messageoutcomeManuel = validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.MANUAL_PROCESSING }
                if (messageoutcomeManuel)
                {
                    MESSAGE_OUTCOME_COUNTER.labels(PaleConstant.eiaMan.string).inc()
                }
                MESSAGE_OUTCOME_COUNTER.labels(PaleConstant.eiaOk.string).inc()
                val fagmelding = pdfClient.generatePDFBase64(PdfType.FAGMELDING, mapFellesformatToFagmelding(fellesformat))
                val behandlingsvedlegg = pdfClient.generatePDFBase64(PdfType.BEHANDLINGSVEDLEGG, mapFellesformatToBehandlingsVedlegg(fellesformat, validationResult.outcomes))
                val joarkRequest = createJoarkRequest(fellesformat, fagmelding, behandlingsvedlegg,
                        messageoutcomeManuel)
                journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                if (validationResult.outcomes.none { it.outcomeType == OutcomeType.PATIENT_HAS_SPERREKODE_6 }) {

                    //TODO REMOVE AFTER TESTING STAGE
                        var outcomesString =  ""
                        validationResult.outcomes.forEach{ outcomesString += it.outcomeType.name + ", "}
                        log.info("validationResult.outcomes {}, $defaultKeyFormat",
                                keyValue("outcomes" , outcomesString),
                                *defaultKeyValues)

                    log.info("Sending " + {if (messageoutcomeManuel) {"manuel"} else {"auto"} } + "message to arena $defaultKeyFormat", *defaultKeyValues)
                    arenaProducer.send(session.createTextMessage().apply {
                        val arenaEiaInfo = createArenaEiaInfo(fellesformat, validationResult.tssId, null, validationResult.navkontor )
                        val stringWriter = StringWriter()
                        arenaEiaInfoMarshaller.marshal(arenaEiaInfo, stringWriter)
                        text = arenaEiaInfoMarshaller.toString(arenaEiaInfo)
                        arenaProducer.send(this)
                    })
                } else {
                    log.info("Not sending message to arena $defaultKeyFormat", *defaultKeyValues)
                }

                log.info("Sending OK apprec for $defaultKeyFormat", *defaultKeyValues)
                receiptProducer.send(session.createTextMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.ok)
                    text = apprecMarshaller.toString(apprec)
                    receiptProducer.send(this)
                    APPREC_STATUS_COUNTER.labels(ApprecStatus.ok.dn).inc()
                })
            }
           requestLatency.observeDuration()
        } catch (e: Exception) {
            log.error("Exception caught while handling message, sending to backout $defaultKeyFormat", *defaultKeyValues, e)
            backoutProducer.send(it)
        }
    }

    while (isActive) {
        delay(100)
    }
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

fun List<Outcome>.toResult(tssId: String? = null, navkontor: String? = null) =
        ValidationResult(tssId, this, navkontor)

fun validateMessage(fellesformat: EIFellesformat, personV3: PersonV3, orgnaisasjonEnhet: OrganisasjonEnhetV2, sarClient: SarClient): ValidationResult {
    val legeerklaering = extractLegeerklaering(fellesformat)
    val outcomes = mutableListOf<Outcome>()

    outcomes.addAll(validationFlow(fellesformat))

    outcomes.addAll(preTPSFlow(fellesformat))

    if (outcomes.any { it.outcomeType.shouldReturnEarly() })
        return outcomes.toResult()

    val patientIdent = extractPersonIdent(legeerklaering)!!
    val patientIdentType = if (isDNR(patientIdent)) {
        PaleConstant.DNR.string
    } else {
        PaleConstant.FNR.string
    }

    val personDeferred = retryWithInterval(retryInterval, "hent_person") {
        personV3.hentPerson(HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(
                        NorskIdent()
                                .withIdent(extractPersonIdent(legeerklaering)!!)
                                .withType(Personidenter().withValue(patientIdentType)))
                ).withInformasjonsbehov(Informasjonsbehov.FAMILIERELASJONER)).person
    }

    val person = try { runBlocking {
        personDeferred.await()
            }
    } catch (e: HentPersonPersonIkkeFunnet) {
        // TODO:Add tests for adding apprec on this error
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
                this.value = geografiskTilknytningDeferred.await().geografiskTilknytning
            }
        }).navKontor
    }

    outcomes.addAll(postNORG2Flow(runBlocking { navKontorDeferred.await() }))
    if (outcomes.any { it.outcomeType.shouldReturnEarly() }) {
        return outcomes.toResult()
    }
    val samhandler = runBlocking { samhandlerDeferred.await() }

    outcomes.addAll(postSARFlow(fellesformat, samhandler))

    if (outcomes.isEmpty()) {
        outcomes += OutcomeType.LEGEERKLAERING_MOTTAT.toOutcome()
    }

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
                it.samh_praksis_periode.any {
                    it.gyldig_fra <= LocalDateTime.now() && (it.gyldig_til == null || it.gyldig_til >= LocalDateTime.now())
                }
            }
            .filter {!it.navn.isNullOrEmpty() }
            .toList()

        return aktiveSamhandlere
                .map {
                    SamhandlerPraksisMatch(it, calculatePercentageStringMatch(it.navn, orgName))
                }.sortedBy { it.percentageMatch }
                .firstOrNull()
    }

fun calculatePercentageStringMatch(str1: String?, str2: String): Double {
    var percentageStringMatch = 0.0
    if (!str1.isNullOrBlank()) {
        val maxDistance = max(str1?.length!!, str2.length).toDouble()
        val distance = LevenshteinDistance().apply(str2, str1).toDouble()
        percentageStringMatch = (maxDistance - distance) / maxDistance
    }
    return percentageStringMatch
}

fun <T> retryWithInterval(interval: Array<Long>, callName: String, blocking: suspend () -> T): Deferred<T> {
    return async {
        for (time in interval) {
            try {
                WS_CALL_TIME.labels(callName).startTimer().use {
                    return@async blocking()
                }
            } catch (e: IOException) {
                log.warn("Caught IO exception trying to reach {}", callName, e)
            }
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
    // sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = fasitProperties.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun getHCPFodselsnummer(fellesformat: EIFellesformat): String? =
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident
                .find { it.typeId.v == "FNR" }?.id ?: ""

fun sha256hashstring(legeerklaering: Legeerklaring): String {
    val bytes = objectMapper.writeValueAsBytes(legeerklaering)
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}
