package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

/**
 * Torrent Torrent
 */

public class Torrent {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "Torrent";

    private static final int PIECE_HASH_SIZE = 20;

    private byte[] mInfoHash = null;
    private int mFileLength = 0;
    private int mPieceLength = 0;
    private ByteBuffer mPieceHashes = null;
    private final ArrayList<String> mTrackerList = new ArrayList<>();
    private final String mName;

    public static Torrent createFromInputStream(InputStream in) {
        Torrent client = null;
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

            if (infoMap.containsKey("files")) {
                throw new UnsupportedOperationException("We can't support multi files yet !! T.T");
            }

            // int file fileLength
            Object length = infoMap.get("length");
            if (length == null || length instanceof Integer == false) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to get torrent length");
                }
                return null;
            }

            // piece length
            Object pieceLength = infoMap.get("piece length");
            if (pieceLength == null || pieceLength instanceof Integer == false) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to get piece length");
                }
                return null;
            }

            // piece hashes
            Object pieceHash = infoMap.get("pieces");
            if (pieceHash == null || pieceHash instanceof ByteBuffer == false) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to get piece hashes");
                }
                return null;
            }

            // torrent name
            Object name = infoMap.get("name");
            if (name == null || name instanceof ByteBuffer == false) {
                if (DEBUG) {
                    Log.e(TAG, "createFromInputStream(), failed to get name");
                }
                return null;
            }

            client = new Torrent(infoHash, trackerList, (Integer) length,
                    (Integer) pieceLength, (ByteBuffer) pieceHash, new String(((ByteBuffer)name).array(), "UTF-8"));
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "createFromInputStream(), failed decode", e);
            }
        }
        return client;
    }

    public ArrayList<String> getTrackerList() {
        return mTrackerList;
    }

    public byte[] getInfoHash() {
        return mInfoHash;
    }

    public int getFileLength() {
        return mFileLength;
    }

    public int getPieceLength() {
        return mPieceLength;
    }

    public int getMaxIndex() {
        return mPieceHashes.capacity() / PIECE_HASH_SIZE;
    }

    @NonNull
    public String getName() {
        return mName;
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

    private Torrent(@NonNull byte[] infoHash, @NonNull ArrayList<String> tackerList,
                    int length, int pieceLength, @NonNull ByteBuffer pieces,
                    @NonNull String name) {
        mInfoHash = infoHash;
        mFileLength = length;
        mPieceLength = pieceLength;
        mPieceHashes = pieces;
        mTrackerList.addAll(tackerList);
        mName = name;
    }
}
