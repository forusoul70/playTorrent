package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Torrent peer
 */

public class Peer {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "Peer";

    private final Connection mConnection;

    public Peer(String host, int port) throws ConnectException {
        mConnection = Connection.fromAddress(host, port);
        if (mConnection == null) {
            throw new ConnectException("Failed to create connection");
        }
    }

    @WorkerThread
    public void connect(byte[] infoHash, byte[] peerId) throws IOException, InvalidKeyException, InterruptedException {
        if (ValidationUtils.isEmptyArray(infoHash) || ValidationUtils.isEmptyArray(peerId)) {
            return;
        }

        if (mConnection.connect() == false) {
            if (DEBUG) {
                Log.e(TAG, "Connect to peer failed");
            }
            return;
        }

        final ByteArrayOutputStream receivedBytes = new ByteArrayOutputStream();
        final CountDownLatch latch = new CountDownLatch(1);
        Connection.ConnectionListener listener = new Connection.ConnectionListener() {
            @Override
            public void onReceived(@NonNull byte[] received) {
                try {
                    receivedBytes.write(received);
                } catch (IOException ignore) {

                } finally {
                    if (receivedBytes.size() >= HandshakeMessage.BASE_HANDSHAKE_LENGTH) {
                        latch.countDown();
                    }
                }
            }
        };

        // hand shake
        mConnection.addConnectionListener(listener);
        try {
            HandshakeMessage handshake = new HandshakeMessage(infoHash, peerId);
            mConnection.sendMessage(handshake.getMessage());
            latch.await(1, TimeUnit.MINUTES);

            // validate handshake
            HandshakeMessage response = HandshakeMessage.parseFromResponse(ByteBuffer.wrap(receivedBytes.toByteArray()));
            if (response == null || handshake.validateHandShake(response) == false) {
                if (DEBUG) {
                    Log.e(TAG, "connect(), Failed to handshake");
                }
                return;
            }

            if (DEBUG) {
                Log.i(TAG, "Hand shake finished ");
            }
        } finally {
            mConnection.removeListener(listener);
        }
    }
}
