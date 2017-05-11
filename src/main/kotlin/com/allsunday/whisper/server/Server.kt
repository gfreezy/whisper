package com.allsunday.whisper.server

import com.allsunday.whisper.*
import com.allsunday.whisper.proto.*
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aAccept
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ConcurrentHashMap

val logger: Logger = LoggerFactory.getLogger("com.allsunday.whisper.server")


fun log(msg: String) {
//    logger.info("[${Thread.currentThread().name}]: $msg")
    logger.info(msg)
}


class Center {
    val clients = ConcurrentHashMap<Long, Server>()

    fun newLogin(userId: Long, server: Server) {
        clients[userId] = server
    }

    fun logoff(userId: Long) {
        clients.remove(userId)
    }

    fun getClient(userId: Long): Server? {
        return clients[userId]
    }

    fun allClients() = clients.entries
}


open class Server(socketChannel: AsynchronousSocketChannel, private val center: Center) {
    var userId: Long = 0

    val conn = ServerConnection(socketChannel) { bytes ->
        log("Receive: ${bytes.toString(Charset.defaultCharset())}")
        onReceiveMessage(Message.parseFrom(bytes)).toByteArray()
    }

    suspend fun onReceiveMessage(message: Message): Message {
        val data = message.data
        val ack = if (data.`is`(Login::class.java)) {
            onLogin(message.getLogin())
        } else if (data.`is`(TalkToPeer::class.java)) {
            onTalkToPeer(message.getTalkToPeer())
        } else if (data.`is`(Logoff::class.java)) {
            onLogoff(message.getLogoff())
        } else {
            throw RuntimeException()
        }

        return ack.toMessage()
    }

    suspend fun notifyEveryPeer(message: Message) {
        center.allClients().forEach { (uid, client) ->
            launch(Unconfined) {
                log("notify $uid")
                client.conn.newStream().use { stream ->
                    stream.send(message.toByteArray())
                }
            }
        }
    }

    suspend fun onLogin(login: Login): Ack {
        log("login ${login.visitorId}")
        center.newLogin(login.visitorId, this)
        userId = login.visitorId
        notifyEveryPeer(
                LoginNotification
                        .newBuilder()
                        .setUserId(login.visitorId)
                        .build()
                        .toMessage()
        )
        return Ack.newBuilder().setSuccess(true).build()
    }

    suspend fun onTalkToPeer(talkToPeer: TalkToPeer): Ack {
        log("talk from ${talkToPeer.visitorId} to ${talkToPeer.peerId}: ${talkToPeer.message}")
        val peer = center.getClient(talkToPeer.peerId) ?: return Ack.newBuilder().setSuccess(false).build()
        peer.conn.newStream().use {
            val msg = TalkToPeerNotification.newBuilder()
                    .setFromId(talkToPeer.visitorId)
                    .setToId(talkToPeer.peerId)
                    .setMessage(talkToPeer.message)
                    .build()
            it.send(msg.toMessage().toByteArray())
        }
        return Ack.newBuilder().setSuccess(true).build()
    }

    suspend fun onLogoff(logoff: Logoff): Ack {
        log("login ${logoff.visitorId}")
        center.logoff(logoff.visitorId)
        notifyEveryPeer(
                LogoffNotification
                        .newBuilder()
                        .setUserId(logoff.visitorId)
                        .build()
                        .toMessage()
        )
        return Ack.newBuilder().setSuccess(true).build()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking<Unit>(CoroutineName("main")) {
            val server = AsynchronousServerSocketChannel.open()
            server.bind(InetSocketAddress(8888))
            log("Start Listening on 8888")
            val center = Center()
            while (isActive) {
                val socketChannel = server.aAccept()
                log("Accept connection from ${socketChannel.remoteAddress}")
                Server(socketChannel, center)
            }
        }
    }
}
