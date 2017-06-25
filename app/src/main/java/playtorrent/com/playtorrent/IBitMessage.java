package playtorrent.com.playtorrent;

/**
 * Interface bit protocol message
 */

public interface IBitMessage {

    public enum Type {
        INVALID(-2),
        CHOKE(0),
        UNCHOKE(1),
        INTERESTED(2),
        NOT_INTERESTED(3),
        HAVE(4),
        BIT_FIELD(5),
        REQUEST(6),
        PIECE(7),
        CANCEL(8);

        private final int mValue;

        Type(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static Type byValue(int value) {
            switch (value) {
                case 0:
                    return CHOKE;
                case 1:
                    return UNCHOKE;
                case 2:
                    return INTERESTED;
                case 3:
                    return NOT_INTERESTED;
                case 4:
                    return HAVE;
                case 5:
                    return BIT_FIELD;
                case 6:
                    return REQUEST;
                case 7:
                    return PIECE;
                case 8:
                    return CANCEL;
                default:
                    return INVALID;

            }
        }
    }

    byte[] getMessage();
}
