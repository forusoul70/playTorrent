package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

/**
 * Torrent TorrentClient
 */

public class TorrentClient {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "TorrentClient";

    private byte[] mInfoHash = null;
    private final ArrayList<String> mTrackerList = new ArrayList<>();

    public static TorrentClient createFromInputStream(InputStream in) {
        TorrentClient client = null;
        try {
            BitDecoder decoder = BitDecoder.fromInputStream(in);
            Map<String, Object> infoMap = decoder.getDictionary("info");
            if (infoMap == null) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to decode info dictionary");
                }
                return null;
            }

            BitEncoder encoder = new BitEncoder(infoMap);
            byte[] encodedInfo = encoder.encode();
            if (ValidationUtils.isEmptyArray(encodedInfo)) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to encode info hash dictionary");
                }
                return null;
            }

            byte[] infoHash = CipherUtils.sha1(encodedInfo);
            if (ValidationUtils.isEmptyArray(infoHash)) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to get info hash");
                }
                return null;
            }

            // find tracker
            ArrayList<String> trackerList;
            ArrayList<Object> announceList = decoder.getList("announce-list");
            if (ValidationUtils.isEmptyList(announceList) == false) {
                trackerList = convertStringFromObject(announceList);
            } else {
                String tracker = decoder.getString("announce");
                if (TextUtils.isEmpty(tracker)) {
                    if (DEBUG) {
                        Log.e(TAG, "createFromInputStream(), failed to get tracker url");
                    }
                    return null;
                }
                trackerList = new ArrayList<>();
                trackerList.add(tracker);
            }

            client = new TorrentClient(infoHash, trackerList);
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "createFromInputStream(), failed decode", e);
            }
        }
        return client;
    }

    private static ArrayList<String> convertStringFromObject(Object value) throws InvalidClassException, UnsupportedEncodingException {
        if (value == null) {
            return null;
        }

        ArrayList<String> stringArrayList = new ArrayList<>();
        if (value instanceof ArrayList) {
            for (Object object : (ArrayList<?>)value) {
                ArrayList<String> recursiveList = convertStringFromObject(object);
                if (ValidationUtils.isEmptyList(recursiveList) == false) {
                    stringArrayList.addAll(recursiveList);
                }
            }
        } else if (value instanceof ByteBuffer) {
            byte[] stringBytes = ((ByteBuffer)value).array();
            stringArrayList.add(new String(stringBytes, "UTF-8"));
        } else {
            throw new InvalidClassException("value is not array list or string [" + value.getClass() + "]");
        }

        return stringArrayList;
    }

    private TorrentClient(@NonNull byte[] infoHash, @NonNull ArrayList<String> tackerList) {
        mInfoHash = infoHash;
        mTrackerList.addAll(tackerList);
    }

    @VisibleForTesting
    public ArrayList<String> getTrackerList() {
        return mTrackerList;
    }
}
