package com.allsunday.whisper

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.jboss.arquillian.container.test.api.Deployment
import org.jboss.arquillian.junit.Arquillian
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.asset.EmptyAsset
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import java.nio.charset.Charset

@RunWith(Arquillian::class)
class StreamTest {
    @org.junit.Test
    fun send() = runBlocking {
        val channel = Channel<Packet>()
        val channel2 = Channel<Packet>()
        launch(CommonPool) {
            while (isActive) {
                val r = channel2.receive()
                channel.send(r)
            }
        }
        val stream = Stream(1, channel, channel2)
        val data = "hello".toByteArray(Charset.defaultCharset())
        val resp = stream.send(data)
        assertEquals(resp, data)
    }

    companion object {
        @Deployment
        fun createDeployment(): JavaArchive {
            return ShrinkWrap.create(JavaArchive::class.java)
                    .addClass(Stream::class.java)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
        }
    }
}

