package playtorrent.com.playtorrent;

/**
 * Handshake message with peer
 */

public class HandshakeMessage implements IBitMessage {
    private static final String PROTOCOL_IDENTIFIER = "BitTorrent protocol";

    private final byte[] mInfoHash;
    private final int mPeerId;

    @Override
    public byte[] getMessage() {

    }
}
