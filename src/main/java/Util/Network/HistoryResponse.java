package Util.Network;

import User.Model.ChatMessage;
import java.util.List;

public class HistoryResponse extends Packet {
	private final List<ChatMessage> messages;

	public HistoryResponse(List<ChatMessage> messages) {
		this.messages = messages;
	}

	public List<ChatMessage> getMessages() {
		return messages;
	}
}
