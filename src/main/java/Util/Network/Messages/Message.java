package Util.Network.Messages;

import User.Model.User;
import Util.Network.Packet;

/// Oberklasse für alle Benutzernachrichten
public abstract class Message extends Packet {
    private final User sender;

    public Message(User sender) {
        this.sender = sender;
    }

    public User getSender() { return sender; }
}
