package com.allsunday.whisper

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aAccept
import kotlinx.coroutines.experimental.nio.aConnect
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset


class ConnectionTest {
    @Test
    fun clientSend() = runBlocking {
        val server = AsynchronousServerSocketChannel.open()
        server.bind(InetSocketAddress(0))

        launch(Unconfined) {
            while (isActive) {
                val socketChannel = server.aAccept()
                Connection(socketChannel, 0) {
                    it
                }
            }
        }

        val socket = AsynchronousSocketChannel.open()
        socket.aConnect(server.localAddress)
        val conn = Connection(socket, 2) {
            it
        }
        val stream = conn.newStream()
        val req = "hello".toByteArray(Charset.defaultCharset())
        val resp = stream.send(req)
        assertEquals(req.toString(Charset.defaultCharset()), resp.toString(Charset.defaultCharset()))
    }

    @Test
    fun serverPush() = runBlocking {
        val server = AsynchronousServerSocketChannel.open()
        server.bind(InetSocketAddress(0))

        launch(Unconfined) {
            while (isActive) {
                val socketChannel = server.aAccept()
                val conn = Connection(socketChannel, 0) {
                    it
                }
                val stream = conn.newStream()
                val resp = stream.send("pong".toByteArray(Charset.defaultCharset()))
                assertEquals(resp.toString(Charset.defaultCharset()), "pong")
            }
        }

        val socket = AsynchronousSocketChannel.open()
        socket.aConnect(server.localAddress)
        val conn = Connection(socket, 1) {
            assertEquals(it.toString(Charset.defaultCharset()), "pong")
            it
        }
        val stream = conn.newStream()
        val req = "hello".toByteArray(Charset.defaultCharset())
        val resp = stream.send(req)
        stream.close()
        assertEquals(req.toString(Charset.defaultCharset()), resp.toString(Charset.defaultCharset()))
    }
}
