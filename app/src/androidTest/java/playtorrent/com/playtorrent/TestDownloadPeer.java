package playtorrent.com.playtorrent;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Download peer
 */

@RunWith(AndroidJUnit4.class)
public class TestDownloadPeer {
    @Test
    public void testDownloadPeer() throws IOException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        Torrent torrent = Torrent.createFromInputStream(appContext.getAssets().open("ubuntu-17.04-desktop-amd64.iso.torrent"));
        DownloadPeer peer = new DownloadPeer(torrent);
        Assert.assertEquals(20, peer.getPeerId().length());

        peer.start();
    }
}
