package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;

import java.net.ConnectException;

/**
 * Torrent peer
 */

public class Peer {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final Connection mConnection;

    public Peer(String host, int port) throws ConnectException {
        mConnection = Connection.fromAddress(host, port);
        if (mConnection == null) {
            throw new ConnectException("Failed to create connection");
        }
    }

    public void connect() {
        mConnection.connect();
    }

    public void sendMessage(@NonNull IBitMessage message) {

    }
}
