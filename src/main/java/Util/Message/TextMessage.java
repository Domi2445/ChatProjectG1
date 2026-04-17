package Util.Message;

import Util.User;

public class TextMessage extends Message {
    private final String content;
    private final boolean edited;

    public TextMessage(User sender, String content) {
        this(sender, content, false);
    }

    public TextMessage(User sender, String content, boolean edited) {
        super(sender);
        this.content = content;
        this.edited = edited;
    }

    public TextMessage(String messageId, User sender, String content, boolean edited) {
        super(messageId, sender);
        this.content = content;
        this.edited = edited;
    }

    public String getContent() { return content; }

    public boolean isEdited() {
        return edited;
    }
}
