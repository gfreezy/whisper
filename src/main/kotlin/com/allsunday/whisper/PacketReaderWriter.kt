package com.allsunday.whisper

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel


class Packet(val streamId: Int, val data: kotlin.ByteArray)


class PacketReaderWriter(val asynchronousSocketChannel: AsynchronousSocketChannel) {
    private val writeBuf : ByteBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 4)
    private val readBuf : ByteBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 4)

    init {
        readBuf.flip()
        writeBuf.clear()
    }

    suspend fun aWritePacket(packet: Packet) {
        writeBuf.putInt(4 + packet.data.size)
        writeBuf.putInt(packet.streamId)
        writeBuf.put(packet.data)
        writeBuf.flip()
        asynchronousSocketChannel.aWrite(writeBuf)
        writeBuf.clear()
    }

    private suspend fun aRead() {
        readBuf.compact()
        val size = asynchronousSocketChannel.aRead(readBuf)
        if (size == -1) {
            throw RuntimeException("closed")
        }
        readBuf.flip()
    }

    private suspend fun remainingSize() : Int {
        readBuf.remaining()
        return readBuf.limit() - readBuf.position()
    }

    private suspend fun readNBytes(n: Int) {
        while (remainingSize() < n) {
            aRead()
        }
    }

    suspend fun aReadPacket() : Packet {
        val size = aReadSize()
        val command = aReadCommand()
        val data = aReadData(size - 4)
        return Packet(command, data)
    }

    private suspend fun aReadSize() : Int {
        readNBytes(4)
        return readBuf.int
    }

    private suspend fun aReadCommand() : Int {
        readNBytes(4)
        return readBuf.int
    }

    private suspend fun aReadData(size: Int) : kotlin.ByteArray {
        readNBytes(size)
        val byteArray = ByteArray(size)
        readBuf.get(byteArray)
        return byteArray
    }
}
