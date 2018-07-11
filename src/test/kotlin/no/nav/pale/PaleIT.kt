package no.nav.pale

import ai.grakn.redismock.RedisServer
import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import io.ktor.application.call
import io.ktor.content.PartData
import io.ktor.http.ContentType
import io.ktor.request.receiveMultipart
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.runBlocking
import no.nav.model.apprec.AppRec
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.client.PdfClient
import no.nav.pale.client.SarClient
import no.nav.pale.datagen.defaultFellesformat
import no.nav.pale.datagen.defaultNavOffice
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.defaultSamhandler
import no.nav.pale.mapping.ApprecError
import no.nav.pale.utils.assertArenaInfoContains
import no.nav.pale.utils.randomPort
import no.nav.pale.validation.OutcomeType
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.FinnNAVKontorResponse
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kommune
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.ActiveMQServer
import org.apache.activemq.artemis.core.server.ActiveMQServers
import org.apache.commons.io.IOUtils
import org.apache.cxf.BusFactory
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.MessageConsumer
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.TextMessage
import javax.naming.InitialContext
import javax.xml.datatype.DatatypeFactory
import javax.xml.ws.Endpoint

class PaleIT {
    // TODO create more ITs
    private val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

    @Before
    fun resetMocks() {
        reset(personV3Mock, organisasjonEnhetV2Mock)
    }

    fun readAppRec(): AppRec {
        val fellesformat = fellesformatJaxBContext.createUnmarshaller()
                .unmarshal(StringReader(consumeMessage(apprecConsumer))) as EIFellesformat
        return fellesformat.appRec
    }

    fun readArenaEiaInfo(): ArenaEiaInfo = arenaEiaInfoJaxBContext.createUnmarshaller()
                .unmarshal(StringReader(consumeMessage(arenaConsumer))) as ArenaEiaInfo

    @Test
    fun testFullFlowExceptionSendMessageToBOQ() {
        produceMessage(IOUtils.toString(PaleIT::class.java.getResourceAsStream("/legeerklaering.xml"), Charsets.ISO_8859_1))

        val messageOnBoq = consumeMessage(backoutConsumer)

        assertEquals("Should be",
                String(Files.readAllBytes(Paths.get(
                        PaleIT::class.java.getResource("/legeerklaering.xml").toURI())), Charsets.ISO_8859_1),
                messageOnBoq)
    }

    @Test
    fun testPersonOver70() {
        val person = defaultPerson(PersonProperties.ageBetween(71, PersonProvider.MAX_AGE))
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        defaultMocks(person)

        produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.PATIENT_IS_OVER_70)
    }

    @Test
    fun testMessageWithoutErrorsShouldCreateOkAppRec() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        defaultMocks(person)

        produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.LEGEERKLAERING_MOTTAT)
    }

    @Test
    fun testDuplicateReturnsCorrectApprec() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        defaultMocks(person)

        produceMessage(fellesformatString)

        readAppRec()
        readArenaEiaInfo()

        produceMessage(fellesformatString)

        val apprec = readAppRec()

        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.DUPLICATE.s, apprec.error[0].s)
        assertEquals(ApprecError.DUPLICATE.v, apprec.error[0].v)
    }

    @Test
    fun testSperrekode6CausesNoArenaEiaInfo() {
        val person = defaultPerson().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPSF"
            }
        }
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        defaultMocks(person)

        produceMessage(fellesformatString)

        readAppRec()
        assertNull(consumeMessage(arenaConsumer))
    }

    @Test
    fun testSperreKode7CausesArenaMessage() {
        val person = defaultPerson().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPFO"
            }
        }
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        defaultMocks(person)

        produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertEquals(7, arenaEiaInfo.pasientData.spesreg)
    }

    @Test
    fun testPatientMarriedToDoctor() {
        val doctor = defaultPerson()
        val person = defaultPerson().apply {
            this.harFraRolleI.add(
                    Familierelasjon().withTilPerson(doctor)
                            .withTilRolle(Familierelasjoner()
                            .withValue(RelationType.EKTEFELLE.kodeverkVerdi))
            )
        }
        val fellesformat = defaultFellesformat(person = person, doctor = doctor)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        defaultMocks(person)

        produceMessage(fellesformatString)

        readAppRec()
        val arenaEiaInfo = readArenaEiaInfo()
        assertNotNull(arenaEiaInfo)
        assertArenaInfoContains(arenaEiaInfo, OutcomeType.MARRIED_TO_PATIENT)
    }

    @Test
    fun testTPSReturnsMissing() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        `when`(personV3Mock.hentPerson(any())).thenThrow(HentPersonPersonIkkeFunnet("Person ikke funnet", PersonIkkeFunnet()))

        val appRec = readAppRec()

        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_OR_DNUMBER_MISSING_IN_POPULATION_REGISTER.s, appRec.error[0].s)
    }

    @Test
    fun testMissingPatientFirstnameMissingReceipt() {
        val person = defaultPerson().apply {
            personnavn.fornavn = null
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_NAME_IS_NOT_IN_SCHEMA.v, apprec.error[0].v)
    }

    @Test
    fun testMissingPatientSurnameMissingReceipt() {
        val person = defaultPerson().apply {
            personnavn.etternavn = null
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_LASTNAME_IS_NOT_IN_SCHEMA.v, apprec.error[0].v)
    }

    @Test
    fun testMissingPersonNumberReceipt() {
        val person = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = ""
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_NOT_FOUND_IN_SCHEMA.v, apprec.error[0].v)
    }

    @Test
    fun testInvalidPersonNumberReceipt() {
        val person = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = "12345678912"
        }

        val fellesformat = defaultFellesformat(person = person)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.s, apprec.error[0].s)
        assertEquals(ApprecError.PATIENT_PERSON_NUMBER_IS_WRONG.v, apprec.error[0].v)
    }

    @Test
    fun testMissingDoctorPersonNumberReceipt() {
        val doctor = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = ""
        }

        val fellesformat = defaultFellesformat(doctor = doctor)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.s, apprec.error[0].s)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v, apprec.error[0].v)
    }

    @Test
    fun testInvalidDoctorPersonNumberReceipt() {
        val doctor = defaultPerson().apply {
            (aktoer as PersonIdent).ident.ident = "12345678912"
        }

        val fellesformat = defaultFellesformat(doctor = doctor)
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.s, apprec.error[0].s)
        assertEquals(ApprecError.BEHANDLER_PERSON_NUMBER_NOT_VALID.v, apprec.error[0].v)
    }

    @Test
    fun testInvalidGenDateReceipt() {
        val fellesformat = defaultFellesformat().apply {
            msgHead.msgInfo.genDate = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now().plusDays(1)))
        }
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.GEN_DATE_ERROR.s, apprec.error[0].s)
        assertEquals(ApprecError.GEN_DATE_ERROR.v, apprec.error[0].v)
    }

    @Test
    fun testNoNavOfficeCausesMissingPatientInfoReceipt() {
        val person = defaultPerson()
        val fellesformat = defaultFellesformat()
        val fellesformatString = fellesformatJaxBContext.createMarshaller().toString(fellesformat)

        produceMessage(fellesformatString)

        defaultMocks(person)
        reset(organisasjonEnhetV2Mock)

        `when`(organisasjonEnhetV2Mock.finnNAVKontor(any()))
                .thenReturn(FinnNAVKontorResponse().apply {
                    navKontor = null
                })

        val apprec = readAppRec()
        assertEquals("Avvist", apprec.status.dn)
        assertEquals(ApprecError.MISSING_PATIENT_INFO.s, apprec.error[0].s)
        assertEquals(ApprecError.MISSING_PATIENT_INFO.v, apprec.error[0].v)
    }

    fun defaultMocks(person: Person) {
        `when`(personV3Mock.hentPerson(any())).thenReturn(HentPersonResponse().withPerson(person))

        `when`(personV3Mock.hentGeografiskTilknytning(any())).thenReturn(HentGeografiskTilknytningResponse()
                .withAktoer(person.aktoer)
                .withNavn(person.personnavn)
                .withGeografiskTilknytning(Kommune()
                        .withGeografiskTilknytning("navkontor")))

        `when`(organisasjonEnhetV2Mock.finnNAVKontor(any()))
                .thenReturn(FinnNAVKontorResponse().apply {
                    navKontor = defaultNavOffice()
                })
    }

    companion object {
        val log = LoggerFactory.getLogger("pale-it")
        val personV3Mock: PersonV3 = mock(PersonV3::class.java)
        val organisasjonEnhetV2Mock: OrganisasjonEnhetV2 = mock(OrganisasjonEnhetV2::class.java)
        val journalbehandlingMock: Journalbehandling = mock(Journalbehandling::class.java)
        val diagnosisWebServerPort = randomPort()
        val diagnosisWebServerUrl = "http://localhost:$diagnosisWebServerPort"

        private val wsMockPort = randomPort()
        val wsBaseUrl = "http://localhost:$wsMockPort"
        private val mockHttpServerPort = randomPort()
        private val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"

        private lateinit var activeMQServer: ActiveMQServer
        private lateinit var connectionFactory: ConnectionFactory
        private lateinit var queueConnection: Connection
        private lateinit var initialContext: InitialContext

        private lateinit var server: Server

        private lateinit var diagnosisWebserver: ApplicationEngine
        private lateinit var mockWebserver: ApplicationEngine

        private lateinit var producer: MessageProducer
        private lateinit var job: Job

        private lateinit var session: Session

        lateinit var arenaConsumer: MessageConsumer
        lateinit var apprecConsumer: MessageConsumer
        lateinit var backoutConsumer: MessageConsumer

        private val redisServer = RedisServer.newRedisServer()

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Before class")
            redisServer.start()

            activeMQServer = ActiveMQServers.newActiveMQServer(ConfigurationImpl()
                    .setPersistenceEnabled(false)
                    .setJournalDirectory("target/data/journal")
                    .setSecurityEnabled(false)
                    .addAcceptorConfiguration("invm", "vm://0"))
            activeMQServer.start()
            initialContext = InitialContext()
            connectionFactory = initialContext.lookup("ConnectionFactory") as ConnectionFactory

            createJettyServer()
            createHttpMock()

            val personV3Client = JaxWsProxyFactoryBean().apply {
                address = "$wsBaseUrl/ws/tps"
                features.add(LoggingFeature())
                serviceClass = PersonV3::class.java
            }.create() as PersonV3

            val organisasjonEnhetV2Client = JaxWsProxyFactoryBean().apply {
                address = "$wsBaseUrl/ws/norg2"
                features.add(LoggingFeature())
                serviceClass = OrganisasjonEnhetV2::class.java
            }.create() as OrganisasjonEnhetV2

            val journalbehandlingClient = JaxWsProxyFactoryBean().apply {
                address = "$wsBaseUrl/ws/joark"
                features.add(LoggingFeature())
                serviceClass = Journalbehandling::class.java
            }.create() as Journalbehandling

            val pdfClient = PdfClient("$mockHttpServerUrl/create_pdf")
            val jedis = Jedis(redisServer.host, redisServer.bindPort)

            val sarClient = SarClient(mockHttpServerUrl, "username", "password")

            queueConnection = connectionFactory.createConnection()
            queueConnection.start()
            session = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val inputQueue = session.createQueue("input_queue")
            val arenaQueue = session.createQueue("arena_queue")
            val apprecQueue = session.createQueue("apprec_queue")
            val backoutQueue = session.createQueue("backout_queue")

            diagnosisWebserver = createHttpServer(diagnosisWebServerPort, "TEST")

            producer = session.createProducer(inputQueue)

            job = listen(pdfClient, jedis, personV3Client, organisasjonEnhetV2Client, journalbehandlingClient, sarClient,
                    inputQueue, arenaQueue, apprecQueue, backoutQueue, queueConnection)

            arenaConsumer = session.createConsumer(arenaQueue)
            apprecConsumer = session.createConsumer(apprecQueue)
            backoutConsumer = session.createConsumer(backoutQueue)
        }

        fun consumeMessage(consumer: MessageConsumer): String? = consumer.receive(2000).run {
            if (this == null) return null
            if (this !is TextMessage) throw RuntimeException("Got unexpected message type")
            println(this.text)
            this.text
        }

        fun produceMessage(message: String) {
            val textMessage = session.createTextMessage(message)
            producer.send(textMessage)
            log.info("Pushed message to queue")
        }

        private fun createHttpMock() {
            mockWebserver = embeddedServer(Netty, mockHttpServerPort) {
                routing {
                    post("/create_pdf/v1/genpdf/pale/{pdfType}") {
                        call.respondText("Mocked PDF", ContentType.parse("application/pdf"))
                    }

                    post("/input") {
                        val multipart = call.receiveMultipart()
                        log.info("Received input {}", multipart)
                        while (true) {
                            val part = multipart.readPart() ?: break
                            when (part) {
                                is PartData.FileItem -> {
                                    val stream = part.streamProvider()
                                    val string = IOUtils.toString(stream, Charsets.ISO_8859_1)
                                    produceMessage(string)
                                }
                            }
                        }
                    }

                    get("/rest/sar/samh") {
                        val ident = call.request.queryParameters["ident"]
                        call.respondJson {
                            arrayOf(defaultSamhandler(defaultPerson()))
                        }
                    }
                }
            }.start()
        }

        private fun createJettyServer() {
            val soapServlet = CXFNonSpringServlet()

            val servletHandler = ServletContextHandler()
            servletHandler.addServlet(ServletHolder(soapServlet), "/ws/*")

            server = Server(wsMockPort)
            server.handler = servletHandler
            server.start()

            BusFactory.setDefaultBus(soapServlet.bus)
            Endpoint.publish("/tps", personV3Mock)
            Endpoint.publish("/norg2", organisasjonEnhetV2Mock)
            Endpoint.publish("/joark", journalbehandlingMock)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            activeMQServer.stop(true)
            runBlocking {
                job.cancel()
                job.join()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            setup()

            log.info("Diagnosis available at: {}", diagnosisWebServerUrl)
            log.info("Mock and input available at: {}", mockHttpServerUrl)

            Runtime.getRuntime().addShutdownHook(Thread {
                tearDown()
            })
        }
    }
}
