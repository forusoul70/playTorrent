package playtorrent.com.playtorrent;

import android.support.annotation.WorkerThread;
import android.util.Log;

import java.net.ConnectException;

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
    public void connect(byte[] infoHash, byte[] peerId) {
        if (ValidationUtils.isEmptyArray(infoHash) || ValidationUtils.isEmptyArray(peerId)) {
            return;
        }

        if (mConnection.connect() == false) {
            if (DEBUG) {
                Log.e(TAG, "Connect to peer failed");
            }
            return;
        }

        // hand shake
        HandshakeMessage handshake = new HandshakeMessage(infoHash, peerId);
        mConnection.sendMessage(handshake.getMessage());
    }
}
