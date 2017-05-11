package com.allsunday.whisper.client

import com.allsunday.whisper.ClientConnection
import com.allsunday.whisper.Handler
import com.allsunday.whisper.getAck
import com.allsunday.whisper.proto.Ack
import com.allsunday.whisper.proto.Login
import com.allsunday.whisper.proto.Message
import com.allsunday.whisper.proto.TalkToPeer
import com.allsunday.whisper.server.log
import com.allsunday.whisper.toMessage
import javafx.application.Application
import kotlinx.coroutines.experimental.nio.aConnect
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import kotlinx.coroutines.experimental.javafx.JavaFx as UI


class Client(host: String, port: Int) {
    val addr = InetSocketAddress(host, port)
    lateinit var socketChannel: AsynchronousSocketChannel
    lateinit var conn: ClientConnection

    suspend fun connect(callback: Handler) {
        socketChannel = AsynchronousSocketChannel.open()
        socketChannel.aConnect(addr)
        conn = ClientConnection(socketChannel, callback)
    }

    suspend fun login(login: Login): Ack {
        log("login ${login.visitorId}")
        val resp = conn.newStream().use { stream ->
            val message = login.toMessage()
            stream.send(message.toByteArray())
        }
        return parseResp(resp)
    }

    suspend fun talkToPeer(talkToPeer: TalkToPeer): Ack {
        log("talk from ${talkToPeer.visitorId} to ${talkToPeer.peerId}: ${talkToPeer.message}")
        val resp = conn.newStream().use {
            it.send(talkToPeer.toMessage().toByteArray())
        }
        return parseResp(resp)
    }

    private fun parseResp(resp: ByteArray): Ack {
        return Message.parseFrom(resp).getAck()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(MyApp::class.java, *args)
        }
    }
}
