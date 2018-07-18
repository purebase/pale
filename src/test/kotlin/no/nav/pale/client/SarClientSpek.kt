package no.nav.pale.client

import io.ktor.application.call
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.pale.SamhandlerProvider
import no.nav.pale.datagen.defaultPerson
import no.nav.pale.datagen.defaultSamhandlerPraksis
import no.nav.pale.datagen.toSamhandler
import no.nav.pale.respondJson
import no.nav.pale.utils.randomPort
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.itThrows
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.IOException
import java.util.concurrent.TimeUnit

object SarClientSpek : Spek({
    val mockHttpServerPort = randomPort()
    val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    val samhandlerMock = mock<SamhandlerProvider>()
    val mockWebserver = embeddedServer(Netty, mockHttpServerPort) {
        routing {
            get("/rest/sar/samh") {
                call.respondJson(samhandlerMock.getSamhandlerList())
            }
        }
    }.start()
    val sarClient = SarClient(mockHttpServerUrl, "username", "password")

    afterGroup { mockWebserver.stop(1, 10, TimeUnit.SECONDS) }

    val doctor = defaultPerson()
    val samhandler = doctor.toSamhandler(samhandlerPraksisListe = listOf(
            doctor.defaultSamhandlerPraksis(name = "Kule Helsetjenester AS")
    ))

    describe("REST call returns samhandler with praksis") {
        it("Should have the correct name for the samhandler praksis") {
            When calling samhandlerMock.getSamhandlerList() itReturns listOf(samhandler)
            sarClient.getSamhandler("").first().samh_praksis.first().navn shouldEqual "Kule Helsetjenester AS"
        }
    }
    describe("External server fails request") {
        it("Should throw an IOException") {
            When calling samhandlerMock.getSamhandlerList() itThrows RuntimeException("")
            ({ sarClient.getSamhandler("") }) shouldThrow IOException::class
        }
    }
})
