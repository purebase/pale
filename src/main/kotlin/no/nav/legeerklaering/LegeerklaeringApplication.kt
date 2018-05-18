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
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.legeerklaering.client.*
import no.nav.legeerklaering.mapping.*
import no.nav.legeerklaering.metrics.INPUT_MESSAGE_TIME
import no.nav.legeerklaering.metrics.WS_CALL_TIME
import no.nav.legeerklaering.validation.*
import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.*
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Geografi
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.FinnNAVKontorRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.slf4j.LoggerFactory
import javax.xml.datatype.DatatypeFactory
import java.security.MessageDigest
import redis.clients.jedis.Jedis
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import javax.jms.*
import javax.jms.Queue
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller


val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(EIFellesformat::class.java, Legeerklaring::class.java)
val arenaEiaInfoJaxBContext: JAXBContext = JAXBContext.newInstance(ArenaEiaInfo::class.java)
val apprecJaxBContext: JAXBContext = JAXBContext.newInstance(EIFellesformat::class.java, AppRec::class.java)

val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()
val arenaEiaInfoMarshaller: Marshaller = arenaEiaInfoJaxBContext.createMarshaller()
val apprecMarshaller: Marshaller = apprecJaxBContext.createMarshaller()
val newInstance: DatatypeFactory = DatatypeFactory.newInstance()


private val log = LoggerFactory.getLogger(LegeerklaeringApplication::class.java)

class LegeerklaeringApplication

fun <T>todo(reason: String): T {
    throw RuntimeException(reason)
}

fun main(args: Array<String>) {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()

    connectionFactory(fasitProperties).createConnection(fasitProperties.mqUsername, fasitProperties.mqPassword).use {
        connection ->
        connection.start()
        Jedis().use {
            jedis ->
            val session = connection.createSession()
            val inputQueue = session.createQueue(fasitProperties.legeerklaeringQueueName)
            val arenaQueue = session.createQueue(fasitProperties.arenaQueueName)
            val receiptQueue = session.createQueue(todo("Figure what queue to use for apprec"))
            session.close()

            val personV3 = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.virksomhetPersonV3EndpointURL
                features.add(LoggingFeature())
                serviceClass = PersonV3::class.java
            }.create() as PersonV3

            val orgnaisasjonEnhet = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.organisasjonEnhetV2EndpointURL
                features.add(LoggingFeature())
                serviceClass = OrganisasjonEnhetV2::class.java
            }.create() as OrganisasjonEnhetV2

            val journalbehandling = JaxWsProxyFactoryBean().apply {
                todo("Implement fasit resources for joark")
            }.create() as Journalbehandling

            val sarClient = SarClient(fasitProperties.kuhrSarApiEndpointURL, fasitProperties.srvLegeerklaeringUsername, fasitProperties.srvLegeerklaeringPassword)

            listen(PdfClient(fasitProperties.pdfGeneratorEndpointURL), jedis, personV3, orgnaisasjonEnhet, journalbehandling, sarClient, inputQueue, arenaQueue, receiptQueue, connection)
        }
    }

}

fun listen(pdfClient: PdfClient, jedis: Jedis, personV3: PersonV3, organisasjonEnhet: OrganisasjonEnhetV2,
           journalbehandling: Journalbehandling, sarClient: SarClient, inputQueue: Queue, arenaQueue: Queue,
           receiptQueue: Queue, connection: Connection) {
    val session = connection.createSession()
    val consumer = session.createConsumer(inputQueue)
    val arenaProducer = session.createProducer(arenaQueue)
    val receiptProducer = session.createProducer(receiptQueue)

    consumer.setMessageListener {
        if (it is BytesMessage) {
            val bytes = ByteArray(it.bodyLength.toInt())
            it.readBytes(bytes)
            val fellesformat = fellesformatJaxBContext.createUnmarshaller().unmarshal(ByteArrayInputStream(bytes)) as EIFellesformat
            val legeerklaering = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

            val inputHistogram = INPUT_MESSAGE_TIME.startTimer()

            val defaultKeyValues = arrayOf(
                    keyValue("organisationNumber", fellesformat.mottakenhetBlokk.orgNummer),
                    keyValue("ediLoggId", fellesformat.mottakenhetBlokk.ediLoggId),
                    keyValue("msgId", fellesformat.msgHead.msgInfo.msgId)
            )

            val defaultKeyFormat = (0..defaultKeyValues.size).joinToString(", ", "(", ")") { "{}" }

            log.info("Received message from {}, $defaultKeyFormat",
                    keyValue("size", bytes.size),
                    *defaultKeyValues)

            if (log.isDebugEnabled) {
                log.debug("Incoming message {}, $defaultKeyFormat",
                        keyValue("xmlMessage", bytes.toString(Charsets.UTF_8)),
                        *defaultKeyValues)
            }

            val hashValue = createLEHash(legeerklaering)
            val jedisEdiLoggId = jedis.get(hashValue)
            if (jedisEdiLoggId != null) {
                val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                apprec.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.DUPLICAT))
                log.warn("Message with ediloggId {} marked as duplicate $defaultKeyFormat", jedisEdiLoggId,
                        *defaultKeyValues)
            } else {
                jedis.set(hashValue, fellesformat.mottakenhetBlokk.ediLoggId.toString())
            }



            val validationResult = validateMessage(fellesformat, legeerklaering, personV3, organisasjonEnhet, sarClient)

            if (validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.RETUR } || validationResult.tssId == null) {
                session.createBytesMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                    val apprecBytes = apprecMarshaller.toByteArray(apprec)
                    todo<Unit>("Add reason to the apprec")
                    writeBytes(apprecBytes)
                }
            } else {
                val fagmelding = pdfClient.generatePDFBase64(PdfType.FAGMELDING, mapFellesformatToFagmelding(fellesformat))
                val behandlingsvedlegg = pdfClient.generatePDFBase64(PdfType.BEHANDLINGSVEDLEGG, mapFellesformatToBehandlingsVedlegg(fellesformat, validationResult.outcomes))
                val joarkRequest = createJoarkRequest(fellesformat, fagmelding, behandlingsvedlegg)
                journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                log.info("Sending message to arena $defaultKeyFormat", *defaultKeyValues)
                session.createBytesMessage().apply {
                    val arenaEiaInfo = createArenaEiaInfo(fellesformat, validationResult.outcomes, validationResult.tssId)
                    val arenaEiaInfoBytes = arenaEiaInfoMarshaller.toByteArray(arenaEiaInfo)
                    writeBytes(arenaEiaInfoBytes)
                    arenaProducer.send(this)
                }

                log.info("Sending apprec for $defaultKeyFormat", *defaultKeyValues)
                session.createBytesMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.ok)
                    val apprecBytes = apprecMarshaller.toByteArray(apprec)
                    writeBytes(apprecBytes)
                    receiptProducer.send(this)
                }
            }
            inputHistogram.close()
        }
    }
}

fun Marshaller.toByteArray(input: Any): ByteArray = ByteArrayOutputStream().use {
    marshal(input, it)
    it.toByteArray()
}

data class ValidationResult(
        val tssId: String?,
        val outcomes: List<Outcome>
)

fun List<Outcome>.toResult(tssId: String? = null)
        = ValidationResult(tssId, this)

fun validateMessage(fellesformat: EIFellesformat, legeerklaering: Legeerklaring, personV3: PersonV3, orgnaisasjonEnhet: OrganisasjonEnhetV2, sarClient: SarClient): ValidationResult {
    val outcomes = mutableListOf<Outcome>()

    outcomes.addAll(validationFlow(fellesformat))

    outcomes.addAll(preTPSFlow(fellesformat))

    val personDeferred = personV3.hentPersonAsync(legeerklaering.pasientopplysninger.pasient.fodselsnummer)
    val geografiskTilknytningDeferred = personV3.hentGeografiskTilknytningAsync(legeerklaering.pasientopplysninger.pasient.fodselsnummer)
    val samhandlerDeferred = async {
        findBestSamhandlerPraksisMatch(sarClient.getSamhandler(extractDoctorPersonNumberFromSender(fellesformat)))
    }

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
        return outcomes.toResult()
    } catch (e: HentPersonSikkerhetsbegrensning) {
        outcomes += when (e.faultInfo.sikkerhetsbegrensning[0].value) {
            "FP1_SFA" -> OutcomeType.PATIENT_HAS_SPERREKODE_6
            "FP2_FA" -> OutcomeType.PATIENT_HAS_SPERREKODE_7
            else -> throw RuntimeException("Missing handling of FP3_EA/Egen ansatt")
        }
        return outcomes.toResult()
    }

    if (outcomes.none { it.outcomeType.messagePriority == Priority.RETUR }) {
        outcomes.addAll(postTPSFlow(fellesformat, person))
    }

    if (outcomes.none { it.outcomeType.messagePriority == Priority.RETUR }) {
        outcomes.addAll(postNORG2Flow(fellesformat, runBlocking { navKontorDeferred.await() }))
    }

    val samhandlerPraksis = runBlocking { samhandlerDeferred.await() }

    return outcomes.toResult(samhandlerPraksis.tss_ident)
}

fun findBestSamhandlerPraksisMatch(samhandler: List<Samhandler>): SamhandlerPraksis {
    return todo("Implement method for finding the best matched samhandler")
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

fun createLEHash(legeerklaering: Legeerklaring): String {
    val bytes = objectMapper
            .writeValueAsBytes(legeerklaering)

    return createHash(bytes)
}

fun createHash(input: ByteArray): String {
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

fun getHCPFodselsnummer(fellesformat: EIFellesformat): String? =
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident
                .find { it.typeId.v.equals("FNR") }?.id
