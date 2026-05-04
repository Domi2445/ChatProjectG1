package Util.Network;

import java.io.Serial;

public class ReadReceipt extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long messageId;
    private final String username;

    public ReadReceipt(long messageId, String username) {
        this.messageId = messageId;
        this.username = username;
    }

    public long getMessageId() {
        return messageId;
    }

    public String getUsername() {
        return username;
    }
}

