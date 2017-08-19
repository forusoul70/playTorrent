package playtorrent.com.playtorrent

import java.io.IOException

/**
 * File Storage interface
*/

abstract class AbsFileStorage(val fileLength:Long) {
    abstract fun write(bytes:ByteArray, offset:Long)
    @Throws(IOException::class)
    abstract fun get(offset:Long, length:Int):ByteArray
}
