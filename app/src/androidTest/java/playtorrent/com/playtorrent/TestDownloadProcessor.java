package playtorrent.com.playtorrent;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Download peer
 */

@RunWith(AndroidJUnit4.class)
public class TestDownloadProcessor {
    @Before
    public void before() {
        grantExternalStoragePermission();
    }

    @Test
    public void testDownloadPeer() throws IOException, InterruptedException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        Torrent torrent = Torrent.createFromInputStream(appContext.getAssets().open("ubuntu-17.04-server-amd64.iso.torrent"));
        Assert.assertNotNull(torrent);

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

    private void grantExternalStoragePermission() {
        Context context = InstrumentationRegistry.getTargetContext();
        String readPermission = "android.permission.READ_EXTERNAL_STORAGE";
        if (context.checkSelfPermission(readPermission) != PackageManager.PERMISSION_GRANTED) {
            requestGrantPermission(readPermission);
        }

        String writePermission = "android.permission.WRITE_EXTERNAL_STORAGE";
        if (context.checkSelfPermission(writePermission) != PackageManager.PERMISSION_GRANTED) {
            requestGrantPermission(writePermission);
        }
    }

    private void requestGrantPermission(@NonNull String permission) {
        Context context = InstrumentationRegistry.getTargetContext();
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + context.getPackageName() + " " + permission
        );
    }
}
