package playtorrent.com.playtorrent;

/**
 * Un choke message
 */

public class UnChokeMessage implements IBitMessage {
    @Override
    public byte[] getMessage() {
        return new byte[Type.UNCHOKE.getValue()];
    }
}
