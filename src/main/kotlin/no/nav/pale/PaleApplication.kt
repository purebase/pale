package no.nav.pale

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.*
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.pale.client.*
import no.nav.pale.mapping.*
import no.nav.pale.metrics.*
import no.nav.pale.sts.configureSTSFor
import no.nav.pale.validation.*
import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.*
import no.nav.model.pale.Legeerklaring
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Geografi
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.FinnNAVKontorRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.slf4j.LoggerFactory
import javax.xml.datatype.DatatypeFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.jms.*
import javax.jms.Queue
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

private val log = LoggerFactory.getLogger("le-application")

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

            val journalbehandling = JaxWsProxyFactoryBean().apply {
                address = fasitProperties.journalbehandlingEndpointURL
                features.add(LoggingFeature())
                serviceClass = Journalbehandling::class.java
            }.create() as Journalbehandling

            val sarClient = SarClient(fasitProperties.kuhrSarApiEndpointURL, fasitProperties.srvPaleUsername,
                    fasitProperties.srvPalePassword)

            listen(PdfClient(fasitProperties.pdfGeneratorEndpointURL), jedis, personV3, orgnaisasjonEnhet,
                    journalbehandling, sarClient, inputQueue, arenaQueue, receiptQueue, backoutQueue, connection)
                    .join()
        }
    }

}

fun defaultLogInfo(keyValues: Array<StructuredArgument>): String =
        (0..(keyValues.size-1)).joinToString(", ", "(", ")") { "{}" }

fun listen(pdfClient: PdfClient, jedis: Jedis, personV3: PersonV3, organisasjonEnhet: OrganisasjonEnhetV2,
           journalbehandling: Journalbehandling, sarClient: SarClient, inputQueue: Queue, arenaQueue: Queue,
           receiptQueue: Queue, backoutQueue: Queue, connection: Connection) = launch {
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val consumer = session.createConsumer(inputQueue)
    val arenaProducer = session.createProducer(arenaQueue)
    val receiptProducer = session.createProducer(receiptQueue)
    val backoutProducer = session.createProducer(backoutQueue)

    var defaultKeyValues = arrayOf(keyValue("noMessageIdentifier", true))
    var defaultKeyFormat = defaultLogInfo(defaultKeyValues)

    QueueStatusCollector(connection, inputQueue, arenaQueue, receiptQueue, backoutQueue)
            .register<QueueStatusCollector>()

    consumer.setMessageListener {
        var sha256String: String? = null
        var ediLoggId: String? = null
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
            val inputHistogram = INPUT_MESSAGE_TIME.startTimer()

            ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
            sha256String = sha256hashstring(extractLegeerklaering(fellesformat))

            defaultKeyValues = arrayOf(
                    keyValue("organisationNumber", fellesformat.mottakenhetBlokk.orgNummer),
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
            //TODO turn back on duplicate check
           /* if (duplicate) {
                val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                apprec.appRec.error.add(mapApprecErrorToAppRecCV(ApprecError.DUPLICAT))
                log.warn("Message with ediloggId {} marked as duplicate $defaultKeyFormat", jedisSha256String,
                        *defaultKeyValues)
                APPREC_ERROR_COUNTER.labels(ApprecError.DUPLICAT.v).inc()
                receiptProducer.send(session.createBytesMessage().apply {
                    val apprecBytes = apprecMarshaller.toByteArray(apprec)
                    writeBytes(apprecBytes)
                    APPREC_STATUS_COUNTER.labels(ApprecStatus.avvist.dn).inc()
                })
                return@setMessageListener
            }
            */

            val validationResult = try {
                validateMessage(fellesformat, personV3, organisasjonEnhet, sarClient)
            } catch (e: Exception) {
                log.error("Exception caught while validating message, $defaultKeyFormat", *defaultKeyValues, e)
                throw e
            }

            if (validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.RETUR } || validationResult.tssId == null) {
                receiptProducer.send(session.createBytesMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.avvist)
                    apprec.appRec.error.addAll(validationResult.outcomes
                            .filter { it.outcomeType.messagePriority == Priority.RETUR }
                            .map { mapApprecErrorToAppRecCV(it.apprecError!!) }
                    )
                    val apprecBytes = apprecMarshaller.toByteArray(apprec)
                    writeBytes(apprecBytes)
                    APPREC_STATUS_COUNTER.labels(ApprecStatus.avvist.dn).inc()
                })
            } else {
                val fagmelding = pdfClient.generatePDFBase64(PdfType.FAGMELDING, mapFellesformatToFagmelding(fellesformat))
                val behandlingsvedlegg = pdfClient.generatePDFBase64(PdfType.BEHANDLINGSVEDLEGG, mapFellesformatToBehandlingsVedlegg(fellesformat, validationResult.outcomes))
                val joarkRequest = createJoarkRequest(fellesformat, fagmelding, behandlingsvedlegg,
                        validationResult.outcomes.any { it.outcomeType.messagePriority == Priority.MANUAL_PROCESSING })
                journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                if(validationResult.outcomes.none{ it.outcomeType == OutcomeType.PATIENT_HAS_SPERREKODE_6 }) {
                    log.info("Sending message to arena $defaultKeyFormat", *defaultKeyValues)
                    arenaProducer.send(session.createBytesMessage().apply {
                        val arenaEiaInfo = createArenaEiaInfo(fellesformat, validationResult.outcomes, validationResult.tssId)
                        val arenaEiaInfoBytes = arenaEiaInfoMarshaller.toByteArray(arenaEiaInfo)
                        writeBytes(arenaEiaInfoBytes)
                        arenaProducer.send(this)
                    })
                }
                else{
                    log.info("Not sending message to arena $defaultKeyFormat", *defaultKeyValues)
                }

                log.info("Sending apprec for $defaultKeyFormat", *defaultKeyValues)
                receiptProducer.send(session.createBytesMessage().apply {
                    val apprec = createApprec(fellesformat, ApprecStatus.ok)
                    val apprecBytes = apprecMarshaller.toByteArray(apprec)
                    writeBytes(apprecBytes)
                    receiptProducer.send(this)
                    APPREC_STATUS_COUNTER.labels(ApprecStatus.ok.dn).inc()
                })
            }
            inputHistogram.close()
        } catch (e: Exception) {
            log.error("Exception caught while handling message, sending to backout $defaultKeyFormat", *defaultKeyValues, e)
            backoutProducer.send(it)
        }

        if (sha256String != null && ediLoggId != null) {
            jedis.setex(sha256String, TimeUnit.DAYS.toSeconds(7).toInt(), ediLoggId)
        }
    }

    while (isActive) {
        delay(100)
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

    val person = try {
        runBlocking { personDeferred.await() }
    } catch (e: HentPersonPersonIkkeFunnet) {
        outcomes += OutcomeType.PATIENT_NOT_FOUND_TPS
        val apprec = createApprec(fellesformat, ApprecStatus.avvist)
        apprec.appRec.error += mapApprecErrorToAppRecCV(ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER)
        APPREC_ERROR_COUNTER.labels(ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER.v).inc()
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
        findBestSamhandlerPraksis(sarClient.getSamhandler(extractDoctorIdentFromSender(fellesformat)!!.id), fellesformat)
    }

    val navKontorDeferred = retryWithInterval(retryInterval, "finn_nav_kontor") {
        orgnaisasjonEnhet.finnNAVKontor(FinnNAVKontorRequest().apply {
            this.geografiskTilknytning = Geografi().apply {
                this.value = geografiskTilknytningDeferred.await().geografiskTilknytning
            }
        }).navKontor
    }

    outcomes.addAll(postNORG2Flow(fellesformat, runBlocking { navKontorDeferred.await() }))
    if (outcomes.any { it.outcomeType.shouldReturnEarly() }) {
        return outcomes.toResult()
    }

    val samhandlerPraksisMatch = runBlocking { samhandlerDeferred.await() }

    if (samhandlerPraksisMatch == null) {
        outcomes += OutcomeType.BEHANDLER_NOT_TSS
    } else {
        outcomes.addAll(postTSSFlow(fellesformat, samhandlerPraksisMatch.samhandlerPraksis))
    }


    if (outcomes.isEmpty()){
        outcomes+= OutcomeType.LEGEERKLAERING_MOTTAT
    }

    return outcomes.toResult(samhandlerPraksisMatch?.samhandlerPraksis?.tss_ident)
}

data class SamhandlerPraksisMatch(val samhandlerPraksis: SamhandlerPraksis, val percentageMatch: Double)

fun findBestSamhandlerPraksis(samhandlers: List<Samhandler>, fellesformat: EIFellesformat): SamhandlerPraksisMatch? {
    val aktiveSamhandlere = samhandlers.flatMap { it.samh_praksis }
            .filter {
                it.samh_praksis_status_kode == "aktiv"
            }
            .filter {
                it.samh_praksis_periode.any {
                    it.gyldig_fra >= LocalDateTime.now() && (it.gyldig_til == null || it.gyldig_til <= LocalDateTime.now())
                }
            }
            //.filter {
            //    it.samh_praksis_type_kode in arrayOf("LEVA", "LEKO", "FALE")
            //}
            //.filter {
            //    it.samh_praksis_status_kode == "LE"
            //}
            .toList()
    if (aktiveSamhandlere.size == 1)
        return SamhandlerPraksisMatch(aktiveSamhandlere.first(), 100.0)

    val orgName = extractSenderOrganisationName(fellesformat)
    return aktiveSamhandlere
            .map {
                SamhandlerPraksisMatch(it, calculatePercentageStringMatch(it.navn, orgName))
            }.sortedBy { it.percentageMatch }
            .first()
}

fun calculatePercentageStringMatch(str1: String, str2: String): Double {
    val maxDistance = max(str1.length, str2.length).toDouble()
    val distance = LevenshteinDistance().apply(str2, str1).toDouble()
    return (maxDistance - distance) / maxDistance
}

fun <T>retryWithInterval(interval: Array<Long>, callName: String, blocking: suspend () -> T): Deferred<T> {
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
    //sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = fasitProperties.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun getHCPFodselsnummer(fellesformat: EIFellesformat): String? =
        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident
                .find { it.typeId.v == "FNR" }?.id


fun sha256hashstring(legeerklaering: Legeerklaring): String {
    val duplicateChekedFields =  StringBuilder()

    duplicateChekedFields.append(legeerklaering.legeerklaringGjelder.first()?.typeLegeerklaring)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger?.flereArbeidsforhold)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient?.fodselsnummer)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient?.trygdekontor)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.navn?.etternavn)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.navn?.fornavn)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.navn?.mellomnavn)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold?.yrkeskode)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold?.primartArbeidsforhold)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet?.virksomhetsAdr)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet.virksomhetsAdr?.postalAddress)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet.virksomhetsAdr.postalAddress.first()?.streetAddress)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet.virksomhetsAdr.postalAddress.first()?.postalCode)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet.virksomhetsAdr.postalAddress.first()?.city)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet.virksomhetsAdr.postalAddress.first()?.country)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet.virksomhetsAdr?.teleinformasjon)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet?.virksomhetsBetegnelse)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold.virksomhet?.organisasjonsnummer)
    duplicateChekedFields.append(legeerklaering.pasientopplysninger.pasient.arbeidsforhold?.yrkesbetegnelse)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle.henvistBehandling?.henvistDato)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle.henvistBehandling?.antattVentetid)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle.henvistBehandling?.spesifikasjon)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle.henvistUtredning?.henvistDato)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle.henvistUtredning?.antattVentetid)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle.henvistUtredning?.spesifikasjon)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle?.nyeLegeopplysninger)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle?.ikkeVidereBehandling)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle?.nyVurdering)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle?.behandlingsPlan)
    duplicateChekedFields.append(legeerklaering.planUtredBehandle?.utredningsPlan)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet?.arbeidsuforFra)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.diagnoseKodesystem?.kodesystem)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.diagnoseKodesystem?.enkeltdiagnose)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.diagnoseKodesystem.enkeltdiagnose.first()?.diagnose)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.diagnoseKodesystem.enkeltdiagnose.first()?.kodeverdi)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.diagnoseKodesystem.enkeltdiagnose.first()?.sortering)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.vurderingYrkesskade?.borVurderes)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet.vurderingYrkesskade?.skadeDato)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet?.statusPresens)
    duplicateChekedFields.append(legeerklaering.diagnoseArbeidsuforhet?.symptomerBehandling)
    duplicateChekedFields.append(legeerklaering.forslagTiltak?.tiltak)
    duplicateChekedFields.append(legeerklaering.forslagTiltak?.aktueltTiltak)
    duplicateChekedFields.append(legeerklaering.forslagTiltak.aktueltTiltak.first()?.typeTiltak)
    duplicateChekedFields.append(legeerklaering.forslagTiltak.aktueltTiltak.first()?.hvilkeAndreTiltak)
    duplicateChekedFields.append(legeerklaering.forslagTiltak?.opplysninger)
    duplicateChekedFields.append(legeerklaering.forslagTiltak?.begrensningerTiltak)
    duplicateChekedFields.append(legeerklaering.forslagTiltak?.begrunnelseIkkeTiltak)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne?.arbeidssituasjon)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.arbeidssituasjon.first()?.arbeidssituasjon)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.arbeidssituasjon.first()?.annenArbeidssituasjon)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne?.funksjonsevne)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne?.kravArbeid)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.vurderingArbeidsevne?.gjenopptaArbeid)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.vurderingArbeidsevne?.narGjenopptaArbeid)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.vurderingArbeidsevne?.taAnnetArbeid)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.vurderingArbeidsevne?.narTaAnnetArbeid)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.vurderingArbeidsevne?.ikkeGjore)
    duplicateChekedFields.append(legeerklaering.vurderingFunksjonsevne.vurderingArbeidsevne?.hensynAnnetYrke)
    duplicateChekedFields.append(legeerklaering.prognose?.bedreArbeidsevne)
    duplicateChekedFields.append(legeerklaering.prognose?.antattVarighet)
    duplicateChekedFields.append(legeerklaering.prognose?.varighetFunksjonsnedsettelse)
    duplicateChekedFields.append(legeerklaering.prognose?.varighetNedsattArbeidsevne)
    duplicateChekedFields.append(legeerklaering.arsakssammenhengLegeerklaring)
    duplicateChekedFields.append(legeerklaering.forbeholdLegeerklaring?.tilbakeholdInnhold)
    duplicateChekedFields.append(legeerklaering.forbeholdLegeerklaring?.borTilbakeholdes)
    duplicateChekedFields.append(legeerklaering.andreOpplysninger?.onskesKopi)
    duplicateChekedFields.append(legeerklaering.andreOpplysninger?.opplysning)
    duplicateChekedFields.append(legeerklaering.kontakt.first()?.kontakt)
    duplicateChekedFields.append(legeerklaering.kontakt.first()?.annenInstans)

    val bytes = duplicateChekedFields.toString().toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}