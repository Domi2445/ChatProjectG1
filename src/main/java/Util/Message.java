package Util;

import java.time.LocalDateTime;

public class Message {
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return sender + "|" + content;
    }

    public static Message fromString(String raw) {
        String[] parts = raw.split("\\|", 2);
        if (parts.length < 2) throw new IllegalArgumentException("Ungültiges Format: " + raw);
        return new Message(parts[0], parts[1]);
    }
}