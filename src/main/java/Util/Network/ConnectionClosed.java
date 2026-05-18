package Util.Network;

import java.io.Serial;

public class ConnectionClosed extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String reason;

	public ConnectionClosed(String reason) {
		this.reason = reason;
	}

	public String getReason() {
		return reason;
	}
}
