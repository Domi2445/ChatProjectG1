package Util.Network;

import java.io.Serial;
import java.io.Serializable;

public class HistoryRequest extends Packet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String sender;
    private String receiver;
    private String chatRoomId;

    // Konstruktoren
    public HistoryRequest() {}

    public HistoryRequest(String sender, String receiver, String chatRoomId) {
        this.sender = sender;
        this.receiver = receiver;
        this.chatRoomId = chatRoomId;
    }

    // Getter und Setter
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    @Override
    public String toString() {
        return "HistoryRequest{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", chatRoomId='" + chatRoomId + '\'' +
                '}';
    }
}
