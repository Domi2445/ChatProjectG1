package Util.Network;

import java.io.Serial;

public class DeleteMessage extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final long messageId;

	public DeleteMessage(long messageId) {
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}
}
