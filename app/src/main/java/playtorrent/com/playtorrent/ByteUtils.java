package playtorrent.com.playtorrent;

import android.util.Log;

import java.util.Locale;

/**
 * Byte utils
 */

public class ByteUtils {
    private static final String TAG = "ByteUtils";

    public static void printByteBuffer(byte[] buffer) {
        if (BuildConfig.DEBUG) {
            // Debug
            StringBuilder builder = new StringBuilder();
            int index = 0;
            while (index < buffer.length) {
                for (int i = 0; i < 8; i++) {
                    if (index >= buffer.length) {
                        break;
                    }
                    builder.append(String.format(Locale.getDefault(), "0x%02x", buffer[index]));
                    builder.append(" ");
                    index++;
                }
                builder.append("\n");
            }
            Log.d(TAG, builder.toString());
            Log.d(TAG, "[" + buffer.length + "]");
        }
    }
}
