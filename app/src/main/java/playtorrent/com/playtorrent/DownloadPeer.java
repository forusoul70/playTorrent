package playtorrent.com.playtorrent;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
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

        infoHash = CipherUtils.sha1(infoHash);
        String encodingCharset = "ISO-8859-1"; // keep origin bytes
        for (String tracker : trackerList) {
            StringBuilder urlBuilder = new StringBuilder(tracker);
            try {
                urlBuilder.append(tracker.contains("?") ? "&" : "?")
                        .append("info_hash=").append(URLEncoder.encode(new String(infoHash, encodingCharset), encodingCharset))
                        .append("&peer_id=").append(URLEncoder.encode(new String(getPeerId().getBytes(), encodingCharset), encodingCharset))
                        .append("&peer_id=").append(URLEncoder.encode(new String(getPeerId().getBytes(), encodingCharset), encodingCharset))
                        .append("&port=").append(49152)
                        .append("&upload=").append(0)
                        .append("&downloaded=").append(0)
                        .append("&left=").append(mTorrent.getFileLength())
                        .append("&event=started");

                BufferedInputStream in = null;
                try {
                    URL url = new URL(urlBuilder.toString());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    in = new BufferedInputStream(connection.getInputStream());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int readCount = -1;
                    byte[] buffer = new byte[1024];
                    while ((readCount = in.read(buffer)) > 0) {
                        out.write(buffer, 0, readCount);
                    }

                    Log.e(TAG, "GET tracker : " + new String(out.toByteArray()));
                } catch (MalformedURLException e) {
                    if (DEBUG) {
                        Log.e(TAG, "findTracker(), Failed to build tracker url", e);
                    }
                    return;
                } catch (IOException e) {
                    if (DEBUG) {
                        Log.e(TAG, "findTracker(), failed to make connection", e);
                    }
                    return;
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ignore) {

                        }
                    }
                }


            } catch (UnsupportedEncodingException e) {
                if (DEBUG) {
                    Log.e(TAG, "findTracker() failed", e);
                }
            }
        }
    }
}
