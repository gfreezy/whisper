package com.allsunday.whisper

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset


class StreamTest {
    @Test
    fun send() = runBlocking {
        val channel = Channel<Packet>()
        val channel2 = Channel<Packet>()
        launch(Unconfined) {
            while (isActive) {
                val r = channel2.receive()
                channel.send(r)
            }
        }
        val stream = Stream(1, channel2, channel)
        val data = "hello".toByteArray(Charset.defaultCharset())
        val resp = stream.send(data)
        assertEquals(resp, data)
    }
}
