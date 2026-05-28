package Util.Network.Messages;

import Util.Network.Packet;
import User.Model.User;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public abstract class Message extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    private final User sender;
    private final Set<String> readByUsernames;
    private final long messageId;
    private final LocalDateTime sentAt;

    public Message(User sender) {
        this.sender = sender;
        this.readByUsernames = new HashSet<>();
        this.messageId = System.nanoTime();
        this.sentAt = LocalDateTime.now();
    }

    public Message(User sender, long messageId, LocalDateTime sentAt) {
        this.sender = sender;
        this.readByUsernames = new HashSet<>();
        this.messageId = messageId;
        this.sentAt = sentAt;
    }

    public User getSender() { return sender; }

    public void markAsReadBy(String username) {
        readByUsernames.add(username);
    }

    public Set<String> getReadByUsernames() {
        return new HashSet<>(readByUsernames);
    }

	public long getMessageId() {
        return messageId;
    }

	public LocalDateTime getSentAt() {
        return sentAt;
    }
}
