import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberServerSocketChannel
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import java.net.InetSocketAddress
import java.nio.charset.Charset

class Server {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val server = ServerActor(9999)
            server.spawn()
            server.get()
        }
    }
}


class ServerActor(val port: Int) : Actor() {
    init {
        name = "server"
    }

    override fun close() {
        unregister()
        super.close()
    }

    @Suspendable override fun doRun() {
        register("server")
        val fiberServerSocketChannel = FiberServerSocketChannel.open()
        fiberServerSocketChannel.bind(InetSocketAddress(port))
        var count = 0
        while (true) {
            val socketChannel = fiberServerSocketChannel.accept()
            val remoteAddr = socketChannel.remoteAddress
            count += 1
            println("Accept connection from ${remoteAddr.toString()} $count")
            ClientActor(socketChannel).spawn()
        }
    }
}


class ClientActor(val socketChannel: FiberSocketChannel) : Actor() {
    init {
        name = "client"
    }

    @Suspendable override fun doRun() {
        println("spawn client Actor")
        var userName: String? = null
        var userActor: ActorRef<Any?>? = null

        val frameReaderWriter = FrameReaderWriter(socketChannel)

        while (true) {
            val frame = frameReaderWriter.readFrame() ?: continue

            if (userName == null) { // first frame is user name
                userName = frame.toString(Charset.defaultCharset())
                userActor = UserActor(userName, frameReaderWriter).spawn()
                link(userActor)
            } else {  // proxy frame to user actor
                userActor?.send(Msg.Receive(frame.toString(Charset.defaultCharset())))
            }
        }
    }
}


class UserActor(name: String, val frameReaderWriter: FrameReaderWriter) : Actor() {
    init {
        setName(name)
        register(name)
    }

    override fun close() {
        unregister()
        super.close()
    }

    @Suspendable override fun doRun() {
        println("spawn client UserActor")


        while (true) {
            receive {
                when (it) {
                    is Msg.Receive -> {
                        val msg = it.msg
                        if (msg.contains(':', true)) {
                            val segments = msg.split(":")
                            val receiverName = segments[0]
                            val message = segments[1]
                            if (receiverName != name) {
                                println("send to $receiverName: $it")
                                val receiver = ActorRegistry.getActor<ActorRef<Any?>>(receiverName)
                                receiver?.send(Msg.Send(message))
                            }
                        }
                    }
                    is Msg.Send -> {
                        val msg = it.msg
                        frameReaderWriter.writeFrame(msg.toByteArray(Charset.defaultCharset()))
                    }
                }
            }
        }
    }
}

sealed class Msg {
    class Receive(val msg: String) : Msg()
    class Send(val msg: String) : Msg()
}
