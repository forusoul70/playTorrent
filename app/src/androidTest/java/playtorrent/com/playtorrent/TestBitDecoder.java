package playtorrent.com.playtorrent;

import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test bit decoder
 */

@RunWith(AndroidJUnit4.class)
public class TestBitDecoder {
    @Test
    public void decodeTorrentFile() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        BitDecoder decoder = BitDecoder.fromInputStream(appContext.getAssets().open("ubuntu-17.04-desktop-amd64.iso.torrent"));
        assertTrue(decoder.getString("announce") != null);
    }
}
