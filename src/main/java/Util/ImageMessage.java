package Util;

public class ImageMessage extends  Message
{
        private final byte[] content;

        public ImageMessage(User sender, byte[] content) {
            super(sender);
            this.content = content;
        }

        public byte[] getBytes() {
            return content;
        }

    @Override
    public String getContent() {
        return "";
    }

    @Override
    public void send()
    {

    }
}
