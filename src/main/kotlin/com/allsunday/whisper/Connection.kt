package com.allsunday.whisper

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ConcurrentHashMap

open class Connection(asynchronousSocketChannel: AsynchronousSocketChannel, var initStreamId: Int, val handler: Handler) {
    val packetReaderWriter: PacketReaderWriter = PacketReaderWriter(asynchronousSocketChannel)
    val streams: ConcurrentHashMap<Int, Channel<Packet>> = ConcurrentHashMap()
    val outChannel: Channel<Packet> = Channel<Packet>()
    var streamId: Int = initStreamId

    init {
        keepReceiving()
        keepSending()
    }

    fun newStream(): Stream {
        if (streamId >= Int.MAX_VALUE - 10) {
            streamId = initStreamId
        }
        streamId += 2

        val channel = Channel<Packet>()
        streams.put(streamId, channel)
        return Stream(streamId, outChannel, channel)
    }

    private fun keepSending() = launch(Unconfined) {
        while (!outChannel.isClosedForReceive && isActive) {
            val packet = outChannel.receive()
            packetReaderWriter.aWritePacket(packet)
        }
    }

    private fun keepReceiving() = launch(Unconfined) {
        while (isActive) {
            val packet = packetReaderWriter.aReadPacket()
            val stream = streams[packet.streamId]
            if (stream != null) {
                if (!stream.isClosedForSend) {
                    stream.send(packet)
                } else {
                    streams.remove(packet.streamId)
                }
            } else {
                outChannel.send(Packet(packet.streamId, handler(packet.data)))
            }
        }
    }
}
