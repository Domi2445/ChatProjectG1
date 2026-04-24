package Util.Network.Messages;

import Util.Network.Packet;
import User.Model.User;

import java.util.UUID;

/// Oberklasse für alle Benutzernachrichten
public abstract class Message extends Packet {
    private final User sender;
	private UUID groupId;

	public Message(User sender)
	{
		this.sender = sender;
		this.groupId = null;
	}

    public User getSender() { return sender; }
	public UUID getGroupId() { return groupId; }
	public void setGroupId(UUID groupId) { this.groupId = groupId; }
}
