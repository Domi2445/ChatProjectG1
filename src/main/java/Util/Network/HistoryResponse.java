package Util.Network;

import User.Model.ChatMessage;
import java.util.List;

public class HistoryResponse extends Packet {
	private final List<ChatMessage> messages;
	private final String status;
	private final String errorMessage;

	public HistoryResponse(List<ChatMessage> messages) {
		this.messages = messages;
		this.status = "success";
		this.errorMessage = null;
	}

	public HistoryResponse(List<ChatMessage> messages, String status, String errorMessage) {
		this.messages = messages;
		this.status = status;
		this.errorMessage = errorMessage;
	}

	public List<ChatMessage> getMessages() {
		return messages;
	}

	public String getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
