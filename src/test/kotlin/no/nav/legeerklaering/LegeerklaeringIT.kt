package no.nav.legeerklaering

import ai.grakn.redismock.RedisServer
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
import no.nav.legeerklaering.client.PdfClient
import no.nav.legeerklaering.client.Samhandler
import no.nav.legeerklaering.client.SarClient
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
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
import org.junit.BeforeClass
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import javax.jms.ConnectionFactory
import javax.naming.InitialContext
import javax.xml.ws.Endpoint

class LegeerklaeringIT {

    companion object {
        val log = LoggerFactory.getLogger("le.legeerklaering-it")
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
        private lateinit var initialContext: InitialContext

        private lateinit var server: Server

        private lateinit var diagnosisWebserver: ApplicationEngine
        private lateinit var mockWebserver: ApplicationEngine

        private val redisServer = RedisServer.newRedisServer()

        @BeforeClass
        fun setup() {
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

            val queueConnection = connectionFactory.createConnection()
            queueConnection.start()
            val session = queueConnection.createSession()
            val inputQueue = session.createQueue("input_queue")
            val arenaQueue = session.createQueue("arena_queue")
            val apprecQueue = session.createQueue("apprec_queue")
            val backoutQueue = session.createQueue("backout_queue")

            diagnosisWebserver = createHttpServer(diagnosisWebServerPort, "TEST")

            session.close()
            listen(pdfClient, jedis, personV3Client, organisasjonEnhetV2Client, journalbehandlingClient, sarClient, inputQueue, arenaQueue, apprecQueue, backoutQueue, queueConnection)
        }

        private fun createHttpMock() {
            mockWebserver = embeddedServer(Netty, mockHttpServerPort) {
                routing {
                    post("/{pdfType}") {
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
                                    val bytes = IOUtils.toByteArray(stream)

                                    connectionFactory.createConnection().use {
                                        it.start()

                                        val session = it.createSession()
                                        val inputQueue = session.createQueue("input_queue")
                                        val bytesMessage = session.createBytesMessage()
                                        bytesMessage.writeBytes(bytes)
                                        val producer = session.createProducer(inputQueue)

                                        producer.send(bytesMessage)
                                        log.info("Pushed message to queue")
                                    }
                                }
                            }
                        }
                    }

                    get("/rest/sar/samh") {
                        val ident = call.request.queryParameters["ident"]
                        call.respondJson {
                            arrayOf<Samhandler>()
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
        fun tearDown() {
            activeMQServer.stop(true)
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
