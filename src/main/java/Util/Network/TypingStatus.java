package Util.Network;

import User.Model.User;

import java.io.Serial;

public class TypingStatus extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final User sender;
	private final boolean typing;

	public TypingStatus(User sender, boolean typing) {
		this.sender = sender;
		this.typing = typing;
	}

	public User getSender() {
		return sender;
	}

	public boolean isTyping() {
		return typing;
	}
}
