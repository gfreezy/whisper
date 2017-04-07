package com.allsunday.whisper

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aAccept
import kotlinx.coroutines.experimental.nio.aConnect
import kotlinx.coroutines.experimental.runBlocking
import org.jboss.arquillian.container.test.api.Deployment
import org.jboss.arquillian.junit.Arquillian
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.asset.EmptyAsset
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset

@RunWith(Arquillian::class)
class ConnectionTest {
    @org.junit.Test
    fun clientSend() = runBlocking {
        val server = AsynchronousServerSocketChannel.open()
        server.bind(InetSocketAddress(0))

        launch(CommonPool) {
            while (isActive) {
                val socketChannel = server.aAccept()
                val conn = Connection(socketChannel, 0) {
                    it
                }
                conn.keepReceiving()
                conn.keepSending()
            }
        }

        val socket = AsynchronousSocketChannel.open()
        socket.aConnect(server.localAddress)
        val conn = Connection(socket, 2) {
            it
        }
        conn.keepSending()
        conn.keepReceiving()
        val stream = conn.newStream()
        val req = "hello".toByteArray(Charset.defaultCharset())
        val resp = stream.send(req)
        assertEquals(req.toString(Charset.defaultCharset()), resp.toString(Charset.defaultCharset()))
    }

    @org.junit.Test
    fun serverPush() = runBlocking {
        val server = AsynchronousServerSocketChannel.open()
        server.bind(InetSocketAddress(0))

        launch(CommonPool) {
            while (isActive) {
                val socketChannel = server.aAccept()
                val conn = Connection(socketChannel, 0) {
                    it
                }
                conn.keepReceiving()
                conn.keepSending()
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
        conn.keepSending()
        conn.keepReceiving()
        val stream = conn.newStream()
        val req = "hello".toByteArray(Charset.defaultCharset())
        val resp = stream.send(req)
        stream.close()
        assertEquals(req.toString(Charset.defaultCharset()), resp.toString(Charset.defaultCharset()))
    }

    companion object {
        @Deployment
        fun createDeployment(): JavaArchive {
            return ShrinkWrap.create(JavaArchive::class.java)
                    .addClass(Connection::class.java)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
        }
    }
}
