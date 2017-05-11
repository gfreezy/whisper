package com.allsunday.whisper

import java.nio.channels.AsynchronousSocketChannel

class ServerConnection(asynchronousSocketChannel: AsynchronousSocketChannel, handler: Handler)
    : Connection(asynchronousSocketChannel, 1, handler) {

}
