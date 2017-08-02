package playtorrent.com.playtorrent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by lee on 2017. 5. 29..
 */
public class BitDecoderTest {
    @Test
    public void byteIntTest() {
        int length = ByteUtils.getInt32(new byte[]{0, 0, 1, (byte) 0x81});
        assertEquals(385, length);
    }
}