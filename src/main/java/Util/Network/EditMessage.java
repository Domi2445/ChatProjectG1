package Util.Network;

import java.io.Serial;

public class EditMessage extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final long messageId;
	private final String newContent;

	public EditMessage(long messageId, String newContent) {
		this.messageId = messageId;
		this.newContent = newContent;
	}

	public long getMessageId() {
		return messageId;
	}

	public String getNewContent() {
		return newContent;
	}
}
