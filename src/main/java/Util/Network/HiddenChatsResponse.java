package Util.Network;

import java.util.List;

public class HiddenChatsResponse extends Packet {
    private static final long serialVersionUID = 1L;
    private final List<String> chatRefs;

    public HiddenChatsResponse(List<String> chatRefs) {
        this.chatRefs = chatRefs;
    }

    public List<String> getChatRefs() { return chatRefs; }
}
