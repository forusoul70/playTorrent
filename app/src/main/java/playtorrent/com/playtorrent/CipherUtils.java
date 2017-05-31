package playtorrent.com.playtorrent;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cipher utils
 */

public class CipherUtils {

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "CipherUtils";

    public static byte[] sha1(byte data[]) {
        if (data == null || data.length == 0) {
            return null;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            if (DEBUG) {
                Log.e(TAG, "CipherUtils() failed", e);
            }
        }
        return null;
    }
}
