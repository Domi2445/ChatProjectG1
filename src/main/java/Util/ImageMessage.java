package Util;

public class ImageMessage extends  Message
{
    private byte[] content;
    public ImageMessage(User sender, byte[] content) {
        super(sender);
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public void send()
    {

    }
}
