package com.allsunday.whisper.server

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.nio.aAccept
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel

val logger: Logger = LoggerFactory.getLogger("com/allsunday/whisper/server")

fun log(msg: String) {
    logger.info("[${Thread.currentThread().name}]: $msg")
}


class Server {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking<Unit>(CoroutineName("main")) {
            val server = AsynchronousServerSocketChannel.open()
            server.bind(InetSocketAddress(8888))
            log("Start Listening on 8888")
            while (isActive) {
                val socketChannel = server.aAccept()
                log("Accept connection from ${socketChannel.remoteAddress}")
            }
        }
    }
}
