package com.allsunday.whisper

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ConcurrentHashMap

typealias Handler = (ByteArray) -> ByteArray

class Connection(asynchronousSocketChannel: AsynchronousSocketChannel, var streamId: Int, val handler: Handler) {
    val packetReaderWriter: PacketReaderWriter = PacketReaderWriter(asynchronousSocketChannel)
    val streams: ConcurrentHashMap<Int, Channel<Packet>> = ConcurrentHashMap()
    val outChannel: Channel<Packet> = Channel<Packet>()

    fun newStream(): Stream {
        streamId += 2
        val channel = Channel<Packet>()
        streams.put(streamId, channel)
        return Stream(streamId, channel, outChannel)
    }

    suspend fun keepSending() = launch(CommonPool) {
        while (!outChannel.isClosedForReceive && isActive) {
            val packet = outChannel.receive()
            packetReaderWriter.aWritePacket(packet)
        }
    }

    suspend fun keepReceiving() = launch(CommonPool) {
        while (isActive) {
            val packet = packetReaderWriter.aReadPacket()
            val stream = streams[packet.streamId]
            if (stream != null) {
                stream.send(packet)
            } else {
                outChannel.send(Packet(packet.streamId, handler(packet.data)))
            }
        }
    }
}
