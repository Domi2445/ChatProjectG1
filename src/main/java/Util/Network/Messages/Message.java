package Util.Network.Messages;

import Util.Network.Packet;
import Util.User;

/// Oberklasse für alle Benutzernachrichten
public abstract class Message extends Packet {
    private final User sender;

    public Message(User sender) {
        this.sender = sender;
    }

    public User getSender() { return sender; }
}
