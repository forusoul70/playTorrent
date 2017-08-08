package playtorrent.com.playtorrent;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Download peer
 */

public class DownloadProcessor {
    public interface DownloadListener {
        void onDownloadStarted();
    }

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "DownloadProcessor";

    /** Default block fileLength is 2^14 bytes, or 16kB. */
    public static final int DEFAULT_REQUEST_SIZE = 16384;

    /** Max block request fileLength is 2^17 bytes, or 131kB. */
    public static final int MAX_REQUEST_SIZE = 131072;

    private final Torrent mTorrent;
    private final String mPeerId;

    private final ConcurrentHashMap<String, Peer> mPeerMap = new ConcurrentHashMap<>();
    private final SparseArray<DownloadPiece> mDownloadMap = new SparseArray<>();

    private final AbsFileStorage mFileStorage;

    private DownloadListener mDownloadListener = null;

    public DownloadProcessor(@NonNull Torrent torrent) throws IOException {
        mTorrent = torrent;
        String uuid[] = UUID.randomUUID().toString().split("-");
        mPeerId = uuid[0] + uuid[1] + uuid[2] + uuid[3]; // length 20;

        // init piece array
        int maxIndex = mTorrent.getMaxIndex();
        int pieceLength = mTorrent.getPieceLength();
        for (int i = 0; i < maxIndex; i++) {
            int size = i != (maxIndex -1) ? pieceLength : mTorrent.getFileLength() % pieceLength;
            Piece piece = new Piece(i, i * pieceLength, size);
            mDownloadMap.put(i, new DownloadPiece(piece));
        }

        // Support single file storage yet
        File saveFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + torrent.getName());
        if (saveFile.exists() == false) {
            if (saveFile.createNewFile() == false) {
                throw new IOException("Failed to make file [" + saveFile.getPath() + "]");
            }
        }
        mFileStorage = new SingleFileStorage(torrent.getFileLength(), saveFile.getPath());
    }

    public void setDownloadListener(DownloadListener listener) {
        mDownloadListener = listener;
    }

    public void start() {
        findTracker();
    }

    public String getPeerId() {
        return mPeerId;
    }

    private void findTracker() {
        ArrayList<String> trackerList = mTorrent.getTrackerList();
        if (ValidationUtils.isEmptyList(trackerList)) {
            if (DEBUG) {
                Log.e(TAG, "findTracker(), tracker list is empty");
            }
            return;
        }

        byte[] infoHash = mTorrent.getInfoHash();
        if (ValidationUtils.isEmptyArray(infoHash)) {
            if (DEBUG) {
                Log.e(TAG, "findTracker(), info hash is empty");
            }
            return;
        }

        String encodingCharset = "ISO-8859-1"; // keep origin bytes
        for (String tracker : trackerList) {
            StringBuilder urlBuilder = new StringBuilder(tracker);
            try {
                urlBuilder.append(tracker.contains("?") ? "&" : "?")
                        .append("info_hash=").append(URLEncoder.encode(new String(infoHash, encodingCharset), encodingCharset))
                        .append("&peer_id=").append(URLEncoder.encode(new String(getPeerId().getBytes(), encodingCharset), encodingCharset))
                        .append("&port=").append(49152)
                        .append("&uploaded=").append(0)
                        .append("&downloaded=").append(0)
                        .append("&left=").append(mTorrent.getFileLength())
                        .append("&compact=").append(1)
                        .append("&event=started");

                try {
                    URL url = new URL(urlBuilder.toString());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    BitDecoder decoder = BitDecoder.fromInputStream(new BufferedInputStream(connection.getInputStream())); // auto close input stream
                    handleResponseTracker(decoder);

                    // for test
                    break;
                } catch (MalformedURLException | InvalidKeyException e ) {
                    if (DEBUG) {
                        Log.e(TAG, "findTracker(), Failed to build tracker url", e);
                    }
                } catch (IOException e) {
                    if (DEBUG) {
                        Log.e(TAG, "findTracker(), failed to make connection", e);
                    }
                } finally {

                }
            } catch (UnsupportedEncodingException e) {
                if (DEBUG) {
                    Log.e(TAG, "findTracker() failed", e);
                }
            }
        }
    }

    private void handleResponseTracker(BitDecoder decodedResponse) {
        if (decodedResponse == null) {
            if (DEBUG) {
                Log.e(TAG, "handleResponseTracker(), Input decoder is null");
            }
            return;
        }

        // check failed
        String failedReason = decodedResponse.getString("failure reason");
        if (TextUtils.isEmpty(failedReason) == false) {
            if (DEBUG) {
                Log.e(TAG, "handleResponseTracker(), Tracker request failed, reason is " + failedReason);
            }
            return;
        }

        byte[] peers = decodedResponse.getByteArray("peers");
        if (ValidationUtils.isEmptyArray(peers)) {
            if (DEBUG) {
                Log.e(TAG, "handleResponseTracker(), Failed to find peer");
            }
            return;
        }

        if (peers.length % 6 != 0) {
            if (DEBUG) {
                Log.e(TAG, "invalid peers format");
            }
            return;
        }

        ByteBuffer peerBuffer = ByteBuffer.wrap(peers);
        int peersLength = peers.length / 6;
        for (int i = 0; i < peersLength; i++) {
            byte[] ipBytes = new byte[4];
            peerBuffer.get(ipBytes);
            try {
                InetAddress ip = InetAddress.getByAddress(ipBytes);
                int port = (0xFF & (int)peerBuffer.get()) << 8 | (0xFF & (int)peerBuffer.get());

                try {
                    Peer downloadPeer = new Peer(ip.getHostAddress(), port);
                    downloadPeer.setPeerListener(mPeerEventListener);
                    mPeerMap.put(ip.getHostAddress(), downloadPeer);
                    break;
                } catch (ConnectException e) {
                    if (DEBUG) {
                        Log.e(TAG, "handleResponseTracker(), Failed to create connection");
                    }
                }

            } catch (UnknownHostException e) {
                if (DEBUG) {
                    Log.e(TAG, "handleResponseTracker(), UnknownHostException occurred", e);
                }
            }
        }

        if (mPeerMap.isEmpty() == false) {
            handFoundPeerList();
        }
    }

    private void handFoundPeerList() {
        if (mPeerMap.isEmpty()) {
            if (DEBUG) {
                Log.e(TAG, "handFoundPeerList(), Current peer list is empty");
            }
            return;
        }

        for (Map.Entry<String, Peer> entry : mPeerMap.entrySet()) {
            Peer downloadPeer = entry.getValue();
            try {
                downloadPeer.connect(mTorrent.getInfoHash(), ByteUtils.getByteEncodingSting(mPeerId));
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to peer");
            }
            break;
        }
    }

    @VisibleForTesting()
    public void connectLocalHostPeer() throws UnknownHostException, ConnectException {
        // find local address
        InetAddress local = InetAddress.getLocalHost();
        Peer downloadPeer = new Peer("10.0.2.2", 49152);
        mPeerMap.put(local.getHostAddress(), downloadPeer);
        downloadPeer.setPeerListener(mPeerEventListener);
        handFoundPeerList();
    }

    private DownloadPiece getNextDownloadPiece() {
        synchronized (mDownloadMap) {
            int maxIndex = mTorrent.getMaxIndex();
            for (int i = 0; i < maxIndex; i++) {
                DownloadPiece piece = mDownloadMap.get(i);
                if (piece.getNextDownloadOffset() >= 0) {
                    return piece;
                }
            }
        }
        return null;
    }

    private void executeNextPiece(@NonNull Peer peer) {
        BitFieldMessage bitField = peer.getReceivedBitFieldMessage();
        if (bitField == null) {
            if (DEBUG) {
                Log.i(TAG, "onChockStateChanged(), unchocked but should wait bit field message");
            }
            return;
        }

        if (bitField.cardinality() > 0) {
            requestPiece(peer);
        }
    }

    private void requestPiece(@NonNull Peer peer) {
        DownloadPiece piece = getNextDownloadPiece();
        if (piece == null) {
            if (DEBUG) {
                Log.e(TAG, "onBitFiled(), Failed to find piece to download. Is finished download ?");
            }
            return;
        }

        int requestLength = Math.min(piece.getRemainDownloadLength(), DEFAULT_REQUEST_SIZE);
        peer.requestSendRequestMessage(piece.getPiece(), piece.getNextDownloadOffset(), requestLength);
    }

    private final Peer.PeerEventListener mPeerEventListener = new Peer.PeerEventListener() {
        @Override
        public void onBitFiled(@NonNull Peer peer, @NonNull BitFieldMessage bitField) {
            if (bitField.cardinality() > 0 ) {
                peer.requestSendInterestMessage();
            }

            if (peer.isChocked() == false) {
                executeNextPiece(peer);
            }
        }

        @Override
        public void onPiece(@NonNull Peer peer, @NonNull PieceMessage pieceMessage) {
            synchronized (mDownloadMap) {
                DownloadPiece downloadPiece = mDownloadMap.get(pieceMessage.getPieceIndex());
                if (downloadPiece == null) {
                    if (DEBUG) {
                        Log.e(TAG, "We got piece message but invalid index !! [" + pieceMessage.getPieceIndex() + "]");
                    }
                    return;
                }

                int pieceLength = pieceMessage.getBuffer().length;
                mFileStorage.write(pieceMessage.getBuffer(), pieceMessage.getOffset());
                if (DEBUG) {
                    Log.d(TAG, String.format("Write piece %d to %d", pieceMessage.getOffset(), pieceLength));
                }
                downloadPiece.mDownloadLength += pieceLength;
            }
            executeNextPiece(peer);
        }

        @Override
        public void onChockStateChanged(@NonNull Peer peer, boolean isChocked) {
            if (peer.isChocked() == false) {
                executeNextPiece(peer);
            }
        }
    };

    private static class DownloadPiece {
        private @NonNull final Piece mPiece;
        private int mDownloadLength = 0; // thread safe ??

        DownloadPiece(@NonNull Piece peer) {
            mPiece = peer;
        }

        @NonNull
        Piece getPiece() {
            return mPiece;
        }

        int getNextDownloadOffset() {
            return mPiece.getLength() <= mDownloadLength ? -1 : (mDownloadLength == 0 ? 0 : mDownloadLength + 1);
        }

        int getRemainDownloadLength() {
            return mPiece.getLength() - mDownloadLength;
        }
    }
}
