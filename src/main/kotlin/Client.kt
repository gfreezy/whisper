import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.strands.Strand
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class Client {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val host = args[0]
            val port = args[1]
            val name = args[2]
            val count = args[3]
            for (i in 1..count.toInt()) {
                val ping = PingActor("$name$i", host, port.toInt())
                ping.spawn()
                Strand.sleep(10, TimeUnit.MILLISECONDS)
                println("ping $i")
            }
            Strand.sleep(1000, TimeUnit.SECONDS)
        }
    }
}

class PingActor(name: String, val addr: String, val port: Int) : Actor() {
    init {
        setName(name)
    }

    @Suspendable override fun doRun() {
        val socket = FiberSocketChannel.open(InetSocketAddress(addr, port))
        val frameReaderWriter = FrameReaderWriter(socket)
        println("$name connected")
        var count = 1
        while (true) {
            frameReaderWriter.writeFrame("hello $count from $name".toByteArray(Charset.defaultCharset()))
            count += 1
            Strand.sleep(1, TimeUnit.SECONDS)
        }
//        frameReaderWriter.writeFrame(name.toByteArray(Charset.defaultCharset()))
//
//        val a = ListenActor(frameReaderWriter).spawn()
//        link(a)
//
//        while (true) {
//            val line = readLine()?.trim() ?: continue
//            frameReaderWriter.writeFrame(line.toByteArray(Charset.defaultCharset()))
//            println("${name} sent: $line")
//        }
    }
}


class ListenActor(val frameReaderWriter: FrameReaderWriter) : Actor() {
    @Suspendable override fun doRun() {
        while (true) {
            val msg = frameReaderWriter.readFrame() ?: continue
            println("received: ${msg.toString(Charset.defaultCharset())}")
        }
    }
}
