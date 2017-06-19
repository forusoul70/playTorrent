package playtorrent.com.playtorrent;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Download peer
 */

@RunWith(AndroidJUnit4.class)
public class TestDownloadProcessor {
    @Test
    public void testDownloadPeer() throws IOException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        Torrent torrent = Torrent.createFromInputStream(appContext.getAssets().open("ubuntu-17.04-desktop-amd64.iso.torrent"));
        DownloadProcessor peer = new DownloadProcessor(torrent);
        Assert.assertEquals(20, peer.getPeerId().length());

        peer.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
        }
    }
}
