package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

/**
 * Manage connection with peer
 */

public class Connection {
    static {
        System.loadLibrary("native-lib");
    }

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "Connection";

    private enum ConnectionState {
        IDLE,
        CONNECTED
    }

    private final String mHost;
    private final int mPort;

    private final Object mConnectionLock = new Object();
    private ConnectionState mConnectionState = ConnectionState.IDLE;

    // for native
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
                synchronized (mConnectionLock) {
                    mConnectionState = ConnectionState.CONNECTED;
                }
            }
        };
        mNativeConnectionInstanceId = requestCreate(mConnectionCallback);
    }

    public void connect() {
        requestConnect(mNativeConnectionInstanceId, mHost, mPort);
    }

    public boolean isConnected() {
        synchronized (mConnectionLock) {
            return mConnectionState == ConnectionState.CONNECTED;
        }
    }

    public void sendMessage(@NonNull byte[] message) {
        if (isConnected() == false) {
            if (DEBUG) {
                Log.e(TAG, "Is not connected");
            }
            return;
        }
    }

    // native code 관련
    private interface ConnectionCallback {
        public void onConnected();
    }

    private native int requestCreate(@NonNull ConnectionCallback callback);
    private native void requestConnect(int id, String host, int port);
    private native void requestSendMessage(int id, byte[] message);
}
