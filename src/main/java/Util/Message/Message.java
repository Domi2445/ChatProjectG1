package Util;

import java.io.Serializable;

public abstract class Message implements Serializable {
	private final User sender;

	public Message(User sender) {
		this.sender = sender;
	}

	public User getSender() {
		return sender;
	}
}
