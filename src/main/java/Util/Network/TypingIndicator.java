package Util.Network;

import java.io.Serializable;

public class TypingIndicator extends Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final boolean isTyping;

    public TypingIndicator(String username, boolean isTyping) {
        this.username = username;
        this.isTyping = isTyping;
    }

    public String getUsername() {
        return username;
    }

    public boolean isTyping() {
        return isTyping;
    }
}

