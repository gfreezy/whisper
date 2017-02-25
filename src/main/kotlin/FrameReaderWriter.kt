import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * Created by chaofei on 19/02/2017.
 */
class FrameReaderWriter(val socketChannel: FiberSocketChannel) {
    val readBuf = ByteBuffer.allocateDirect(1024)
    val writeBuf = ByteBuffer.allocateDirect(1024)
    val headerSize = 4
    var readFrameSize = 0

    @Suspendable fun readFrame(): ByteArray? {
        val bytesRead = socketChannel.read(readBuf)
        if (bytesRead == -1) {
            throw IOException("closed")
        }
        readBuf.flip()

        if (readFrameSize == 0 && readBuf.limit() - readBuf.position() >= headerSize) {
            readFrameSize = readBuf.getInt()
        }

        if (readFrameSize > 0 && readBuf.limit() - readBuf.position() >= readFrameSize) {
            val frame = ByteArray(readFrameSize)
            readBuf.get(frame)
            readBuf.compact()
            readFrameSize = 0
            return frame
        }

        readBuf.compact()

        return null
    }

    @Suspendable fun writeFrame(byteArray: ByteArray) {
        writeBuf.putInt(byteArray.count())
        writeBuf.put(byteArray)
        writeBuf.flip()
        socketChannel.write(writeBuf)
        writeBuf.clear()
    }
}
