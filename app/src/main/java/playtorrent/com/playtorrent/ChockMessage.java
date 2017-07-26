package playtorrent.com.playtorrent;

import java.nio.ByteBuffer;

/**
 * Chock message
 */

public class ChockMessage implements IBitMessage {
    @Override
    public byte[] getMessage() {
        // No payload message
        ByteBuffer buffer = ByteBuffer.allocate(5); // 4(message length byte) + 1(message type byte)
        buffer.putInt(1);
        buffer.put((byte) Type.CHOKE.getValue());
        return buffer.array();
    }

    @Override
    public Type getType() {
        return null;
    }
}
