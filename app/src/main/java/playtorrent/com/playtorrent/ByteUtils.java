package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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

    public static String decodeByteEncodeString(byte[] str) {
        if (ValidationUtils.isEmptyArray(str)) {
            return null;
        }

        try {
            return new String(str, BYTE_ENCODING);
        } catch (UnsupportedEncodingException ignore) {

        }
        return null;
    }

    public static int getInt32(@NonNull byte[] bytes) {
        return bytes[3] & 0xff |
                (bytes[2] & 0xff) << 8 |
                (bytes[1] & 0xff) << 16 |
                (bytes[0] & 0xff) << 24;
    }

    public static void writeInt32(@NonNull OutputStream os, int value) throws IOException {
        os.write((value >> 24) & 0xff);
        os.write((value >> 16) & 0xff);
        os.write((value >> 8) & 0xff);
        os.write(value & 0xff);
    }

    public static byte[] toArray(@NonNull ArrayList<Byte> list, int length) {
        byte[] array = new byte[list.size()];
        for (int i = 0; i < list.size() && i < length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static boolean isEqual(@Nullable byte[] a, @Nullable byte[] b) {
        if (a == null && b == null) { // ???
            return true;
        }

        if ((a != null && b == null) || (a == null && b != null)) {
            return false;
        }

        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }
}
