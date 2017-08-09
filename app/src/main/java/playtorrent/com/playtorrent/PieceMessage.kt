package playtorrent.com.playtorrent

import android.util.Log
import java.nio.ByteBuffer

/**
 * Piece message
 */
class PieceMessage(val pieceIndex:Int, val offset:Long, val buffer:ByteArray): IBitMessage {
    companion object {
        private val TAG = "PieceMessage"
        private val DEBUG = BuildConfig.DEBUG
        private val BASE_MESSAGE_LENGTH = 13

        fun parse(message:ByteBuffer, length:Int): PieceMessage? {
            if (length <= BASE_MESSAGE_LENGTH) {
                if (DEBUG) {
                    Log.e(TAG, "Invalid piece message length [$length]")
                }
                return null
            }

            if (message.remaining() != length) {
                if (DEBUG) {
                    Log.e(TAG, "Expected bytes is $length but remaining is ${message.remaining()}")
                }
                return null
            }

            val pieceIndex = message.int
            val offset = message.int
            val buffer = ByteArray(message.remaining())
            message.get(buffer, 0, message.remaining())

            if (DEBUG) {
                Log.d(TAG, "parsed finished. Payload length is ${buffer.size}")
            }

            return PieceMessage(pieceIndex, offset.toLong(), buffer)
        }
    }

    override fun getMessage(): ByteArray {
        val message = ByteBuffer.allocateDirect(BASE_MESSAGE_LENGTH + buffer.size)
        message.putInt(13) // 4 bytes
        message.put(IBitMessage.Type.PIECE.value.toByte()) // 1 byte
        message.putInt(pieceIndex) // 4 bytes
        message.putInt(offset.toInt()) // 4 bytes
        message.putInt(buffer.size)
        return message.array()
    }

    override fun getType(): IBitMessage.Type {
        return IBitMessage.Type.PIECE
    }
}
