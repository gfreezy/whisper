package com.allsunday.whisper

import java.nio.channels.AsynchronousSocketChannel

class ClientConnection(asynchronousSocketChannel: AsynchronousSocketChannel, handler: Handler)
    : Connection(asynchronousSocketChannel, 2, handler)

