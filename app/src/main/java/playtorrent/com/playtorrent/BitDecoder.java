package playtorrent.com.playtorrent;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Bit decoder
 */

public class BitDecoder {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "BitDecoder";
    private final InputStream in;
    private Map<String, Object> infoMaps = null;

    public static BitDecoder fromFilePath(String filePath) throws IOException, InvalidKeyException {
        File file = new File(filePath);
        if (file.exists() == false) {
            throw new FileNotFoundException("Not exist file [" + filePath + "]");
        }

        return new BitDecoder(new FileInputStream(file));
    }

    public static BitDecoder fromInputStream(InputStream in) throws IOException, InvalidKeyException {
        return new BitDecoder(in);
    }

    private BitDecoder(InputStream in) throws IOException, InvalidKeyException {
        this.in = in;
        try {
            decode();
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {

            }
        }
    }

    public String getString(@NonNull String key) {
        if (infoMaps == null || infoMaps.isEmpty()) {
            if (DEBUG) {
                Log.e(TAG, "getString(), info map is empty");
            }
            return null;
        }

        Object value = infoMaps.get(key);
        if (value == null || value instanceof ByteBuffer == false) {
            if (DEBUG) {
                Log.e(TAG, "Failed to get string by [" + key + "]");
            }
            return null;
        }

        try {
            return new String(((ByteBuffer)value).array(), "UTF-8");
        } catch (UnsupportedEncodingException ignore) {

        }
        return null;
    }

    private void decode() throws IOException, InvalidKeyException {
        int type = this.in.read();
        if (type != 'd') {
            throw new InvalidKeyException("We expect dictionary first, but " + (char)type);
        }
        // start decode dictionary
        infoMaps = decodeDictionary(this.in);
    }

    private Object decodeNext(@NonNull InputStream in) throws InvalidKeyException, IOException {
        char type = 0;
        try {
            type = (char) in.read();
        } catch (IOException e) {
            if (DEBUG) {
                Log.d(TAG, "finished");
            }
        }

        if (type == 'i') {
            return decodeInteger(in);
        } else if (type == 'l') {
            return decodeList(in);
        } else if (type >= '0' && type < '9') {
            return decodeBytes(in, type);
        } else if (type == 'd') {
            return decodeDictionary(in);
        } else if (type == 'e') {
            throw new InvalidKeyException("We met end of list");
        } else {
            throw new IOException("Invalid format [" + type + "]");
        }
    }

    private Map<String, Object> decodeDictionary(@NonNull InputStream in) throws IOException, InvalidKeyException {
        Map<String, Object> dictionary = new HashMap<>();

        for (;;) {
            String key = new String(decodeBytes(in).array(), "UTF-8"); // key is always string.
            if (key.length() == 0) {
                break;
            }
            Object value = decodeNext(in);
            if (value == null) {
                break;
            }
            dictionary.put(key, value);
        }

        return dictionary;
    }

    private ArrayList<Object> decodeList(@NonNull InputStream in) throws IOException, InvalidKeyException {
        ArrayList<Object> list = new ArrayList<>();

        Object value;
        for (;;) {
            try {
                value = decodeNext(in);
                list.add(value);
            } catch (InvalidKeyException ignore) { // Decoding next will be failed at end of list. 'e'
                break;
            }
        }
        return list;
    }

    private int decodeInteger(@NonNull InputStream in) throws IOException, NumberFormatException {
        int temp;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while ((temp = in.read()) != 'e') {
            buffer.write(temp);
        }

        return Integer.parseInt(new String(buffer.toByteArray(), "UTF-8"));
    }

    private ByteBuffer decodeBytes(InputStream in) throws IOException {
        return decodeBytes(in, -1);
    }

    private ByteBuffer decodeBytes(@NonNull InputStream in, int prevLoad) throws IOException {
        ByteArrayOutputStream lengthBuffer = new ByteArrayOutputStream();
        if (prevLoad > 0) {
            // we should handle prev loaded string length byte.
            lengthBuffer.write(prevLoad);
        }

        int temp;
        while ((temp = in.read()) != ':') {
            lengthBuffer.write(temp);
        }

        int length = Integer.parseInt(lengthBuffer.toString());
        byte[] byteArray = new byte[length];
        if (in.read(byteArray) != length) {
            if (DEBUG) {
                Log.d(TAG, "Failed to read string");
            }
            return null;
        }

        return ByteBuffer.wrap(byteArray);
    }
}
