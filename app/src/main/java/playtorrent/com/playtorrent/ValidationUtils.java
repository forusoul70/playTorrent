package playtorrent.com.playtorrent;

import java.util.Collection;

/**
 * Check Validation Utils
 */

public class ValidationUtils {
    public static boolean isEmptyArray(byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    public static boolean isEmptyList(Collection<?> list) {
        return list == null || list.isEmpty();
    }
}
