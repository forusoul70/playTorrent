package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Byte utils
 */

public class ByteUtils {
    private static final String TAG = "ByteUtils";
    private static final String BYTE_ENCODING = "ISO-8859-1";

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

    @NonNull
    public static byte[] getByteEncodingSting(String value) {
        if (TextUtils.isEmpty(value)) {
            return new byte[0];
        }

        try {
            return value.getBytes(BYTE_ENCODING);
        } catch (UnsupportedEncodingException ignore) {

        }
        return new byte[0];
    }
}
