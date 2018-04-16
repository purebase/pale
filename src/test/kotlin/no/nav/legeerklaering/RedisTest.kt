package no.nav.legeerklaering

import ai.grakn.redismock.RedisServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import redis.clients.jedis.Jedis
import java.io.IOException
import org.junit.Before


class RedisTest{

    private val server = RedisServer.newRedisServer()

    @Before
    @Throws(IOException::class)
    fun before() {
        server.start()
    }


    @Test
    fun shouldNotFindHashValueInRedis() {

        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val hashValue = LegeerklaeringApplication().createSHA1(inputMeldingFellesformat.toString())

        val jedis = Jedis(server.getHost(), server.getBindPort())

        assertFalse(LegeerklaeringApplication().checkIfHashValueIsInRedis(jedis, hashValue))
    }

    @Test
    fun shouldFindHashValueInRedis() {

        val inputMeldingFellesformat = Utils.readToFellesformat("/legeerklaering.xml")
        val hashValue = LegeerklaeringApplication().createSHA1(inputMeldingFellesformat.toString())

        val jedis = Jedis(server.getHost(), server.getBindPort())

        jedis.set(hashValue, inputMeldingFellesformat.mottakenhetBlokk.ediLoggId.toString())

        assertTrue(LegeerklaeringApplication().checkIfHashValueIsInRedis(jedis, hashValue))
    }


    @After
    fun after() {
        server.stop()
    }

}