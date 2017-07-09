package playtorrent.com.playtorrent;

import java.nio.ByteBuffer;

/**
 * Interested message
 */

public class InterestedMessage implements IBitMessage {

    @Override
    public byte[] getMessage() {
        // No payload message
        ByteBuffer buffer = ByteBuffer.allocate(5); // 4(message length byte) + 1(message type byte)
        buffer.putInt(1);
        buffer.put((byte) Type.INTERESTED.getValue());
        return buffer.array();
    }

    @Override
    public Type getType() {
        return Type.INTERESTED;
    }
}
