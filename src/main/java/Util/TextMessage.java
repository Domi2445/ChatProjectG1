package Util;

public class TextMessage extends Message
{
    public String content;

    public TextMessage(User sender) {
        super(sender);
    }

    @Override
    public void send() {

    }
}
