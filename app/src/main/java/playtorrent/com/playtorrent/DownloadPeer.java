package playtorrent.com.playtorrent;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Download peer
 */

public class DownloadPeer {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "DownloadPeer";

    private final Torrent mTorrent;
    private final String mPeerId;

    public DownloadPeer(@NonNull Torrent torrent) {
        mTorrent = torrent;
        String uuid[] = UUID.randomUUID().toString().split("-");
        mPeerId = uuid[0] + uuid[1] + uuid[2] + uuid[3]; // length 20;
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

                Log.i("YYY", "pared peer = " + ip.toString());
            } catch (UnknownHostException e) {
                if (DEBUG) {
                    Log.e(TAG, "handleResponseTracker(), UnknownHostException occurred", e);
                }
            }
        }
    }
}
