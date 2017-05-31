package playtorrent.com.playtorrent;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.InvalidKeyException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for torrent client
 */

@RunWith(AndroidJUnit4.class)
public class TestTorrentClient {
    @Test
    public void testInitTorrentByFile() throws IOException, InvalidKeyException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        TorrentClient client = TorrentClient.createFromInputStream(appContext.getAssets().open("ubuntu-17.04-desktop-amd64.iso.torrent"));

        assertEquals(2, client.getTrackerList().size());
    }
}
