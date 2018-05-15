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
    val inputMeldingFellesformat = readToFellesformat("/legeerklaering.xml")

    @Before
    @Throws(IOException::class)
    fun before() {
        server.start()
    }


    @Test
    fun shouldNotFindHashValueInRedis() {
        val hashValue = createSha256Hash(objectMapper.writeValueAsBytes(inputMeldingFellesformat.toString()))

        val jedis = Jedis(server.getHost(), server.getBindPort())

        assertFalse(checkIfHashValueIsInRedis(jedis, hashValue))
    }

    @Test
    fun shouldFindHashValueInRedis() {
        val hashValue = createSha256Hash(objectMapper.writeValueAsBytes(inputMeldingFellesformat.toString()))

        val jedis = Jedis(server.getHost(), server.getBindPort())

        jedis.set(hashValue, inputMeldingFellesformat.mottakenhetBlokk.ediLoggId.toString())

        assertTrue(checkIfHashValueIsInRedis(jedis, hashValue))
    }


    @After
    fun after() {
        server.stop()
    }

}
