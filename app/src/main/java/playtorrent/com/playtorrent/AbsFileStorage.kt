package playtorrent.com.playtorrent

/**
 * File Storage interface
*/

abstract class AbsFileStorage(val fileLength:Long) {
    abstract fun write(bytes:ByteArray, offset:Long)
}
