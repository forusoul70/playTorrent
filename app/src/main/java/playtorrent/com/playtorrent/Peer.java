package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Torrent peer
 */

public class Peer {
    public interface PeerEventListener {
        void onBitFiled(@NonNull Peer peer, @NonNull BitFieldMessage bitField);
    }

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "Peer";

    private final Connection mConnection;
    private final Connection.ConnectionListener mConnectionListener;

    // received message thread
    private final ArrayList<Byte> mReceivedMessageBuffer;
    private final ExecutorService mReceiveMessageService;

    private PeerEventListener mListener = null;

    public Peer(String host, int port) throws ConnectException {
        mConnection = Connection.fromAddress(host, port);
        if (mConnection == null) {
            throw new ConnectException("Failed to create connection");
        }

        mConnectionListener = new Connection.ConnectionListener() {
            @Override
            public void onReceived(@NonNull byte[] received) {
                handleReceiveBytes(received);
            }
        };

        mReceiveMessageService = Executors.newFixedThreadPool(1);
        mReceivedMessageBuffer = new ArrayList<>();
    }

    public void setPeerListener(PeerEventListener listener) {
        mListener = listener;
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
            mConnection.addConnectionListener(mConnectionListener);
        } finally {
            mConnection.removeListener(listener);
        }
    }

    void requestSendInterestMessage() {
        mConnection.sendMessage(new InterestedMessage().getMessage());
    }

    void requestSendRequestMessage(@NonNull Piece piece) {
        mConnection.sendMessage(new RequestMessage(piece.getIndex(), piece.getOffset(), piece.getLength()).getMessage());
    }

    private void handleReceiveBytes(@NonNull byte[] receivedBytes) {
        ByteBuffer peerMessage = null;
        synchronized (mReceiveMessageService) {
            for (int i = 0; i < receivedBytes.length; i++) {
                mReceivedMessageBuffer.add(receivedBytes[i]);
            }

            if (mReceivedMessageBuffer.size() < 4) {
                if (DEBUG) {
                    Log.e(TAG, "handleReceiveBytes(), Message buffer is empty");
                }
                return;
            }

            // validate message length
            int length = ByteUtils.get32Int(new byte[] {
                    mReceivedMessageBuffer.get(0), mReceivedMessageBuffer.get(1),
                    mReceivedMessageBuffer.get(2), mReceivedMessageBuffer.get(3),
            });

            int totalLength = length + 4;
            if (mReceivedMessageBuffer.size() < totalLength) {
                if (DEBUG) {
                    Log.e(TAG, "handleReceiveBytes(), Message buffer is empty");
                }
                return;
            }

            peerMessage = ByteBuffer.wrap(ByteUtils.toArray(mReceivedMessageBuffer, totalLength));
            for (int i=0; i < totalLength; i++) {
                mReceivedMessageBuffer.remove(0);
            }
        }

        if (peerMessage != null) {
            handlePeerMessage(peerMessage);
        }
    }

    private void handlePeerMessage(@NonNull ByteBuffer message) {
        int length = message.getInt();
        IBitMessage.Type type = IBitMessage.Type.byValue(message.get());
        if (DEBUG) {
            Log.i(TAG, "handleReceiveBytes() type = " + type + " , length = " + length);
        }
        switch (type) {
            case UNCHOKE:
                break;
            case BIT_FIELD:
                mReceiveMessageService.submit(createHandleBitField(new BitFieldMessage(message)));
                break;
            default:
        }
    }

    private Runnable createHandleBitField(@NonNull final BitFieldMessage bitField) {
        return new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onBitFiled(Peer.this, bitField);
                }
            }
        };
    }
}
