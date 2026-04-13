package Util;

public class TextMessage extends Message
{
    private final String content;

    public TextMessage(User sender, String content) {
        super(sender);
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public void send()
    {
        System.out.println(getSender().getUsername() + " sendet: " + content);
    }
}
