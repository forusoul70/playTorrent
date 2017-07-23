package playtorrent.com.playtorrent

import java.io.File
import java.io.RandomAccessFile

/**
 * Single File Storage
 */

class SingleFileStorage(size:Long, path:String): AbsFileStorage(size) {
    private lateinit var file:RandomAccessFile

    init {
        val targetFile = File(path)
        file = RandomAccessFile(targetFile, "rw")
        if (size != targetFile.length()) {
            file.setLength(size)
        }
    }


    override fun write(bytes: ByteArray, offset: Int) {
        file.write(bytes, offset, bytes.size)
    }
}
