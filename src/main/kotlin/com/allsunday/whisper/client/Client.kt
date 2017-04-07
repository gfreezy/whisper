package com.allsunday.whisper.client

import com.allsunday.whisper.server.log
import kotlinx.coroutines.experimental.nio.aConnect
import kotlinx.coroutines.experimental.runBlocking
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel

class Client {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val host = args[0]
            val port = args[1]
            val number = args[2].toInt()
            val addr = InetSocketAddress(host, port.toInt())
            val socket = AsynchronousSocketChannel.open()
            socket.aConnect(addr)
            log("Connected")
        }
    }
}
