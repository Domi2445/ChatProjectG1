package Util;

public abstract class Message {
    private final User sender;

    public Message(User sender) {
        this.sender = sender;

    }

    public User getSender() { return sender; }


    public abstract void send();
}