package Util.Network;

import User.Model.ChatMessage;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class HistoryResponse extends Packet {
	private final List<ChatMessage> messages;
	/** Map from fileId (as stored in ChatMessage.filePath) to file bytes */
	private final Map<String, byte[]> fileContents;
	/** Map from fileId to file extension (no dot), e.g. "png" or "bin" */
	private final Map<String, String> fileExtensions;
	private final String status;
	private final String errorMessage;

	public HistoryResponse(List<ChatMessage> messages) {
		this(messages, Collections.emptyMap(), Collections.emptyMap(), "success", null);
	}

	public HistoryResponse(List<ChatMessage> messages, Map<String, byte[]> fileContents, Map<String, String> fileExtensions) {
		this(messages, fileContents, fileExtensions, "success", null);
	}

	public HistoryResponse(List<ChatMessage> messages, String status, String errorMessage) {
		this(messages, Collections.emptyMap(), Collections.emptyMap(), status, errorMessage);
	}

	public HistoryResponse(List<ChatMessage> messages, Map<String, byte[]> fileContents, Map<String, String> fileExtensions, String status, String errorMessage) {
		this.messages = messages;
		this.fileContents = fileContents == null ? Collections.emptyMap() : fileContents;
		this.fileExtensions = fileExtensions == null ? Collections.emptyMap() : fileExtensions;
		this.status = status;
		this.errorMessage = errorMessage;
	}

	public List<ChatMessage> getMessages() {
		return messages;
	}

	public Map<String, byte[]> getFileContents() {
		return fileContents;
	}

	public Map<String, String> getFileExtensions() {
		return fileExtensions;
	}

	public String getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
