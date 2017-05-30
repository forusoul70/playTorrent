package playtorrent.com.playtorrent;

import java.io.InputStream;

/**
 * Torrent TorrentClient
 */

public class TorrentClient {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "TorrentClient";

    private byte[] mInfoHash = null;

    public TorrentClient createFrom(InputStream in) {
        TorrentClient client = new TorrentClient();
        try {
            BitDecoder decoder = BitDecoder.fromInputStream(in);


        } catch (Exception e) {

        }

        return client;
    }

    private TorrentClient() {

    }
}
