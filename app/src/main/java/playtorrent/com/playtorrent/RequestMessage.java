package playtorrent.com.playtorrent;

import java.nio.ByteBuffer;

/**
 * Request message
 */

public class RequestMessage implements IBitMessage {

    private final int mIndex;
    private final int mOffset;
    private final int mLength;

    public RequestMessage(int index, int offset, int length) {
        mIndex = index;
        mOffset = offset;
        mLength = length;
    }

    @Override
    public byte[] getMessage() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 + 13);
        buffer.putInt(13); // 4 bytes
        buffer.put((byte) Type.REQUEST.getValue()); // 1 byte
        buffer.putInt(mIndex); // 4 bytes
        buffer.putInt(mOffset); // 4 bytes
        buffer.putInt(mLength); // 4 bytes
        return buffer.array();
    }
}
