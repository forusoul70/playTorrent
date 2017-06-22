package playtorrent.com.playtorrent;


import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Handshake message with peer
 */

public class HandshakeMessage implements IBitMessage {
    private static boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "HandshakeMessage";

    private static final String PROTOCOL_IDENTIFIER = "BitTorrent protocol";
    public static final int BASE_HANDSHAKE_LENGTH = 49;

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

    public static HandshakeMessage parseFromResponse(@NonNull ByteBuffer receivedHandshake) {
        int messageLength = receivedHandshake.get();
        if (messageLength + BASE_HANDSHAKE_LENGTH  > receivedHandshake.remaining() + 1) {
            if (DEBUG) {
                Log.e(TAG, "validateHandShake(), Invalid handshake length");
            }
            return null;
        }

        byte[] identifier = new byte[messageLength];
        receivedHandshake.get(identifier);
        if (PROTOCOL_IDENTIFIER.equals(ByteUtils.decodeByteEncodeString(identifier)) == false) {
            if (DEBUG) {
                Log.e(TAG, "validateHandShake(), Invalid handshake identifier [" + ByteUtils.decodeByteEncodeString(identifier) + "]");
            }
            return null;
        }

        // ignore reserved bytes
        byte[] reserved = new byte[8];
        receivedHandshake.get(reserved);

        byte[] infoHash = new byte[20];
        receivedHandshake.get(infoHash);

        byte[] peerId = new byte[20];
        receivedHandshake.get(peerId);
        return new HandshakeMessage(infoHash, peerId);
    }

    public boolean validateHandShake(@NonNull HandshakeMessage handshakeMessage) {
        if (Arrays.equals(mInfoHash, handshakeMessage.mInfoHash) == false) {
            if (DEBUG) {
                Log.e(TAG, "validateHandShake(), Invalid info hash");
            }
            return false;
        }

        return true;
    }
}
