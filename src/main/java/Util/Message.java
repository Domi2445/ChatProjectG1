package Util;

public abstract class Message<T> {
    private final User sender;
    private final T content;

    public Message(User sender, T content) {
        this.sender = sender;
        this.content = content;
    }

    public User getSender() { return sender; }
    public T getContent() { return content; }

    public abstract void send();
}