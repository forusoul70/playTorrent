package playtorrent.com.playtorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ByteUtils.writeInt32(buffer, 13);
            buffer.write((byte) Type.REQUEST.getValue());// 1 byte
            ByteUtils.writeInt32(buffer,mIndex); // 4 bytes
            ByteUtils.writeInt32(buffer,mOffset); // 4 bytes
            ByteUtils.writeInt32(buffer,mLength); // 4 bytes
            return buffer.toByteArray();
        } catch (IOException ignore) {

        }
        return new byte[0];
    }

    @Override
    public Type getType() {
        return Type.REQUEST;
    }
}
