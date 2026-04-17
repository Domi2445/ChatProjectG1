package Util.Network.Requests;

import java.io.Serial;
import java.util.UUID;

public class FileRequest extends Request {
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID uuid;

	public FileRequest(UUID uuid) {
		this.uuid = uuid;
	}

	public UUID getUuid() {
		return uuid;
	}
}
