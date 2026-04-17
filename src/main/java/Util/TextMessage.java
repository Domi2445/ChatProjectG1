package Util;

public class TextMessage extends Message {
    private final String content;

    public TextMessage(User sender, String content) {
        super(sender);
        this.content = content;
    }

    @Override
    public String getContent() { return content; }

    @Override
    public String getDisplayText() {
        return content != null ? content : "";
    }

    @Override
    public void send() {
        throw new UnsupportedOperationException("Sending TextMessage is not supported yet.");
    }
}
