package Util.Network.Messages;

import Util.Network.Packet;
import Util.User;

import java.io.Serializable;

// Oberklasse für alle Benutzernachrichten
public abstract class Message extends Packet implements Serializable {
    private final User sender;

    public Message(User sender) {
        this.sender = sender;
    }

    public User getSender() { return sender; }
}
