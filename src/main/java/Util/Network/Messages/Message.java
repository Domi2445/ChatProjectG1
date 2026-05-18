package Util.Network.Messages;

import Util.Network.Packet;
import User.Model.User;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class Message extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    private final User sender;
    private final Set<String> readByUsernames;
    private final long messageId;
    private final LocalDateTime sentAt;
    // null = global chat, set this to route the message to a specific group only
    private UUID groupId;

    public Message(User sender) {
        this.sender = sender;
        this.readByUsernames = new HashSet<>();
        this.messageId = System.nanoTime();
        this.sentAt = LocalDateTime.now();
        this.groupId = null;
    }

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }

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
