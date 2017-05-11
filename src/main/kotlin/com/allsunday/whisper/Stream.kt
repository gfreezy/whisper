package com.allsunday.whisper

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel


class Stream(val id: Int, private val sendChannel: SendChannel<Packet>, private val recvChannel: Channel<Packet>) : AutoCloseable {
    suspend fun send(payload: ByteArray): ByteArray {
        sendChannel.send(Packet(id, payload))
        return recvChannel.receive().data
    }

    override fun close() {
        recvChannel.close()
    }
}
