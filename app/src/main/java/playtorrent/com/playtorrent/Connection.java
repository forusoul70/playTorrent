package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;

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

    // listener
    private final ConcurrentLinkedQueue<ConnectionListener> mListenerList = new ConcurrentLinkedQueue<>();

    // for native
    private final ConnectionCallback mConnectionCallback;
    private int mNativeConnectionInstanceId = -1;

    public interface ConnectionListener {
        void onReceived(@NonNull byte[] received);
    }

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
            public void onConnectionLost() {
                synchronized (mConnectionLock) {
                    mConnectionState = ConnectionState.IDLE;
                }
            }

            @Override
            public void onReceived(byte[] rev) {
                if (ValidationUtils.isEmptyArray(rev)) {
                    Log.e(TAG, "onReceived(), received empty message");
                    return;
                }

                for (ConnectionListener listener : mListenerList) {
                    listener.onReceived(rev);
                }
            }
        };
        mNativeConnectionInstanceId = requestCreate(mConnectionCallback);
    }

    public void addConnectionListener(@NonNull ConnectionListener listener) {
        if (mListenerList.contains(listener) == false) {
            mListenerList.add(listener);
        }
    }

    public void removeListener(@NonNull ConnectionListener listener) {

    }

    @WorkerThread
    public boolean connect() {
        synchronized (mConnectionLock) {
            if (mConnectionState == ConnectionState.CONNECTED) {
                return true;
            }
        }

        if (requestConnect(mNativeConnectionInstanceId, mHost, mPort)) {
            synchronized (mConnectionLock) {
                mConnectionState = ConnectionState.CONNECTED;
            }
            return true;
        }

        return false;
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
        requestSendMessage(mNativeConnectionInstanceId, message);
    }

    // native code 관련
    private interface ConnectionCallback {
        void onConnectionLost();
        void onReceived(byte[] rev);
    }

    private native int requestCreate(@NonNull ConnectionCallback callback);
    private native boolean requestConnect(int id, String host, int port);
    private native void requestSendMessage(int id, byte[] message);
}
