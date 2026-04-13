package Util;

public abstract class Message {
    private final User sender;

    public Message(User sender) {
        this.sender = sender;
    }

    public User getSender() { return sender; }

    public abstract String getContent();

    public abstract String serialize();

    public static Message fromString(String raw) {
        String[] parts = raw.split("\\|", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Ungültiges Nachrichtenformat: " + raw);
        }
        String type    = parts[0];
        String sender  = parts[1];
        String payload = parts[2];

        return switch (type) {
            case "TEXT" -> new TextMessage(new User(sender), payload);
            default -> throw new IllegalArgumentException("Unbekannter Nachrichtentyp: " + type);
        };
    }

    @Override
    public String toString() {
        return serialize();
    }
}