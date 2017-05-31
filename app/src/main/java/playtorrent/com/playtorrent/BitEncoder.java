package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Bit protocol encoding
 */

public class BitEncoder {
    private static boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "BitEncoder";

    private final Map<String, Object> valueMap;

    public BitEncoder(Map<String, Object> valueMap) {
        this.valueMap = valueMap;
    }

    public byte[] encode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            encodeMap(out, valueMap);
        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "Failed to encode", e);
            }
            return null;
        }
        return out.toByteArray();
    }

    private void encodeNext(@NonNull OutputStream out, @NonNull Object value) throws UnsupportedOperationException, IOException {
        if (value instanceof Integer) { // long ?
            encodeInteger(out, (Integer)value);
        } else if (value instanceof String) {
            encodeString(out, (String) value);
        } else if (value instanceof ArrayList) {
            encodeList(out, (ArrayList<Object>) value);
        } else if (value instanceof Map) {
            encodeMap(out, (Map<String, Object>)value);
        } else if (value instanceof ByteBuffer) {
            encodeByte(out, (ByteBuffer)value);
        } else {
            throw new UnsupportedOperationException("Invalid class type [" + value.getClass().getCanonicalName() + "]");
        }
    }

    private void encodeMap(@NonNull OutputStream out, @NonNull Map<String, Object> map) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write('d'); // start dictionary

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (TextUtils.isEmpty(key) == false || value != null) {
                encodeString(out, key);
                out.write(':');
                encodeNext(out, value);
            }
        }
        outputStream.write('e'); // end
    }


    private void encodeList(@NonNull OutputStream out, @NonNull ArrayList<Object> list) throws IOException {
        if (list.isEmpty()) {
            throw new IOException("Input list is empty");
        }

        out.write('l'); // start list
        for (Object value : list) {
            if (value != null) {
                encodeNext(out, value);
            }
        }
        out.write('e'); // end of list
    }

    private void encodeInteger(@NonNull OutputStream out, int value) throws IOException {
        out.write('i');
        out.write(String.valueOf(value).getBytes("UTF-8"));
        out.write('e');
    }

    private void encodeString(@NonNull OutputStream out, @NonNull String value) throws IOException {
        if (TextUtils.isEmpty(value)) {
            throw new IOException("Input string is empty");
        }

        String encodeString = String.format(Locale.getDefault(), "%d:%s", value.length(), value);
        out.write(encodeString.getBytes("UTF-8"));
    }

    private void encodeByte(@NonNull OutputStream out, @NonNull ByteBuffer buffer) throws IOException {
        byte[] bytes = buffer.array();
        if (bytes.length == 0) {
            throw new IOException("Input bytes is empty");
        }

        String encodeBytes = String.format(Locale.getDefault(), "%d:", bytes.length);
        out.write(encodeBytes.getBytes("UTF-8"));
        out.write(bytes);
    }
}
