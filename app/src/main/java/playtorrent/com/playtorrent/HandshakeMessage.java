package playtorrent.com.playtorrent;


import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Handshake message with peer
 */

public class HandshakeMessage implements IBitMessage {
    private static boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "HandshakeMessage";

    private static final String PROTOCOL_IDENTIFIER = "BitTorrent protocol";

    private final byte[] mInfoHash;
    private final byte[] mPeerId;

    public HandshakeMessage(@NonNull byte[] infoHash, @NonNull byte[] peerId) {
        this.mInfoHash = infoHash;
        this.mPeerId = peerId;
    }

    @Override
    public byte[] getMessage() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(PROTOCOL_IDENTIFIER.length());
        try {
            os.write(ByteUtils.getByteEncodingSting(PROTOCOL_IDENTIFIER));
            os.write(new byte[8]); // reserved
            os.write(mInfoHash);
            os.write(mPeerId);
            return os.toByteArray();
        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "IO exception ", e);
            }
            return null;
        }
    }
}
