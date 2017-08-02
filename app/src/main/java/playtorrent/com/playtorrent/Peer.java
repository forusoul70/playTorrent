package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
        void onPiece(@NonNull Peer peer, @NonNull PieceMessage pieceMessage);
        void onChockStateChanged(@NonNull Peer peer, boolean isChocked);
    }

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "Peer";

    private final Connection mConnection;
    private final Connection.ConnectionListener mConnectionListener;

    // received message thread
    private final ArrayList<Byte> mReceivedMessageBuffer;
    private final ExecutorService mReceiveMessageService;

    private BitFieldMessage mReceivedBitFieldMessage = null;
    private boolean mIsChocked = true;

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
            requestSendProtocolMessage(handshake);
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

    public boolean isChocked() {
        return mIsChocked;
    }

    @Nullable
    public BitFieldMessage getReceivedBitFieldMessage() {
        return mReceivedBitFieldMessage;
    }

    void requestSendInterestMessage() {
        requestSendProtocolMessage(new InterestedMessage());
    }

    void requestSendRequestMessage(@NonNull Piece piece, int nextOffset, int requestLength) {
        requestSendProtocolMessage(new RequestMessage(piece.getIndex(), nextOffset, requestLength));
    }

    private void requestSendProtocolMessage(@NonNull IBitMessage message) {
        if (DEBUG) {
            Log.i(TAG, "requestSendProtocolMessage(), [" + message.getType() + " ]");
        }
        mConnection.sendMessage(message.getMessage());
    }

    private void handleReceiveBytes(@NonNull byte[] receivedBytes) {
        ByteBuffer peerMessage = null;
        synchronized (mReceiveMessageService) {
            for (int i = 0; i < receivedBytes.length; i++) {
                mReceivedMessageBuffer.add(receivedBytes[i]);
            }

            if (mReceivedMessageBuffer.size() < 4) {
                if (DEBUG) {
                    Log.e(TAG, "handleReceiveBytes(), Message dose not have message length");
                }
                return;
            }

            // validate message length
            int length = ByteUtils.getInt32(new byte[] {
                    mReceivedMessageBuffer.get(0), mReceivedMessageBuffer.get(1),
                    mReceivedMessageBuffer.get(2), mReceivedMessageBuffer.get(3),
            });

            int totalLength = length + 4; // 4 bytes mean length field.
            if (mReceivedMessageBuffer.size() < totalLength) {
                if (DEBUG) {
                    Log.e(TAG, "handleReceiveBytes(), Buffer size is " + mReceivedMessageBuffer.size() + ", but total length is " + totalLength);
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
        int length = message.getInt(); // length contains type
        IBitMessage.Type type = IBitMessage.Type.byValue(message.get());
        if (DEBUG) {
            Log.i(TAG, "handleReceiveBytes() type = " + type + " , length = " + length);
        }
        switch (type) {
            case CHOKE:
                mReceiveMessageService.submit(createHandleChocked(new ChockMessage()));
                break;
            case UNCHOKE:
                mReceiveMessageService.submit(createHandleUnChocked(new UnChokeMessage()));
                break;
            case BIT_FIELD:
                mReceiveMessageService.submit(createHandleBitField(new BitFieldMessage(message)));
                break;
            case PIECE:
                PieceMessage pieceMessage = PieceMessage.Companion.parse(message, length - 1);
                if (pieceMessage == null) {
                    if (DEBUG) {
                        Log.e(TAG, "Failed to parse piece message");
                    }
                    return;
                }
                mReceiveMessageService.submit(createHandlePiece(pieceMessage));
                break;
            default:
        }
    }

    private Runnable createHandleChocked(@NonNull final ChockMessage chockMessage) {
        return new Runnable() {
            @Override
            public void run() {
                if (mIsChocked == false) {
                    mIsChocked = true;
                    if (mListener != null) {
                        mListener.onChockStateChanged(Peer.this, true);
                    }
                }
            }
        };
    }

    private Runnable createHandleUnChocked(@NonNull final UnChokeMessage unChokeMessage) {
        return new Runnable() {
            @Override
            public void run() {
                if (mIsChocked == true) {
                    mIsChocked = false;
                    if (mListener != null) {
                        mListener.onChockStateChanged(Peer.this, false);
                    }
                }
            }
        };
    }

    private Runnable createHandleBitField(@NonNull final BitFieldMessage bitField) {
        return new Runnable() {
            @Override
            public void run() {
                mReceivedBitFieldMessage = bitField;
                if (mListener != null) {
                    mListener.onBitFiled(Peer.this, bitField);
                }
            }
        };
    }

    private Runnable createHandlePiece(@NonNull final PieceMessage pieceMessage) {
        return new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onPiece(Peer.this, pieceMessage);
                }
            }
        };
    }
}
