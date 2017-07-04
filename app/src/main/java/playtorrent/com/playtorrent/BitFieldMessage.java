package playtorrent.com.playtorrent;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Bit message
 */

public class BitFieldMessage implements IBitMessage {
    private final ByteBuffer mBuffer;
    private final ArrayList<Integer> mBitSet;

    public BitFieldMessage(ByteBuffer buffer) {
        mBuffer = buffer;
        mBitSet = new ArrayList<>();
        init();
    }

    private void init() {
        for (int i = 0; i < mBuffer.remaining(); i++) {
            byte b = mBuffer.get();
            mBitSet.add((b & 1) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 1)) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 2)) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 3)) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 4)) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 5)) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 6)) > 0 ? 1 : 0);
            mBitSet.add((b & (1 << 7)) > 0 ? 1 : 0);
        }
    }

    @Override
    public byte[] getMessage() {
        return new byte[0];
    }
}
