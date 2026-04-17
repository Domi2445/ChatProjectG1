package Util.Message;

import Util.User;

import java.io.Serializable;
import java.util.UUID;

public abstract class Message implements Serializable {
    private final String messageId;
    private final User sender;

    public Message(User sender) {
        this(UUID.randomUUID().toString(), sender);
    }

    public Message(String messageId, User sender) {
        this.messageId = messageId;
        this.sender = sender;
    }

    public String getMessageId() {
        return messageId;
    }

    public User getSender() { return sender; }
}
