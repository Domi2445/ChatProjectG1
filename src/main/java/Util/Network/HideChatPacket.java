package Util.Network;

public class HideChatPacket extends Packet {
    private static final long serialVersionUID = 1L;
    private final String chatRef;

    public HideChatPacket(String chatRef) {
        this.chatRef = chatRef;
    }

    public String getChatRef() { return chatRef; }
}
