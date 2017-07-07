package playtorrent.com.playtorrent;

/**
 * Torrent piece
 */

public class Piece {
    private final int index;
    private final int offset;
    private final int length;
    private int downloadLength = 0;

    public Piece(int index, int offset, int length) {
        this.index = index;
        this.offset = offset;
        this.length = length;
    }

    public int getIndex() {
        return index;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public int getDownloadLength() {
        return downloadLength;
    }

    public void setDownloadLength(int downloadLength) {
        this.downloadLength = downloadLength;
    }
}
