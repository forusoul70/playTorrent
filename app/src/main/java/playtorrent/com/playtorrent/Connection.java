package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Manage connection with peer
 */

public class Connection {
    static {
        System.loadLibrary("native-lib");
    }

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final String mHost;
    private final int mPort;
    private final ConnectionCallback mConnectionCallback;
    private int mNativeConnectionInstanceId = -1;

    public static Connection fromAddress(String host, int port) {
        if (TextUtils.isEmpty(host) || port < 0) {
            return null;
        }

        return new Connection(host, port);
    }

    private Connection(@NonNull String host, int port) {
        mHost = host;
        mPort = port;

        mConnectionCallback = new ConnectionCallback() {
            @Override
            public void onConnected() {

            }
        };
        mNativeConnectionInstanceId = requestCreate(this, mConnectionCallback);
    }

    public void connect() {
        requestConnect(mNativeConnectionInstanceId);
    }

    private interface ConnectionCallback {
        public void onConnected();
    }

    private native int requestCreate(@NonNull Connection connection, @NonNull ConnectionCallback callback);
    private native void requestConnect(int id);
}
