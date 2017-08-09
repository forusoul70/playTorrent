package playtorrent.com.playtorrent

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Single File Storage
 */

class SingleFileStorage(size:Long, path:String): AbsFileStorage(size) {
    private val file:RandomAccessFile

    init {
        val targetFile = File(path)
        file = RandomAccessFile(targetFile, "rw")
        if (size != targetFile.length()) {
            file.setLength(size)
        }
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray, offset: Long) {
        file.seek(offset)
        file.write(bytes, 0, bytes.size)
    }
}
