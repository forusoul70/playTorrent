package playtorrent.com.playtorrent;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Download peer
 */

@RunWith(AndroidJUnit4.class)
public class TestDownloadProcessor {
    @Test
    public void testDownloadPeer() throws IOException, InterruptedException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        Torrent torrent = Torrent.createFromInputStream(appContext.getAssets().open("ubuntu-17.04-server-amd64.iso.torrent"));
        DownloadProcessor peer = new DownloadProcessor(torrent);
        Assert.assertEquals(20, peer.getPeerId().length());

        peer.connectLocalHostPeer();

        final CountDownLatch latch = new CountDownLatch(1);
        peer.setDownloadListener(new DownloadProcessor.DownloadListener() {
            @Override
            public void onDownloadStarted() {
                latch.countDown();
            }
        });
        latch.await();
    }
}
