package Util.Network;

import java.io.Serial;

public class DeleteForMeMessage extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final long messageId;

	public DeleteForMeMessage(long messageId) {
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}
}
