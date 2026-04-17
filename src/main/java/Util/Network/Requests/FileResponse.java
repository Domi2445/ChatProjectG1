package Util.Network.Requests;

import Util.Network.Messages.FileMessage;

import java.io.Serial;
import java.util.UUID;

public class FileResponse extends Request {
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID uuid;
	private final String name;
	private final byte[] data;
	private final int size;
	private final FileMessage.FileType type;

	public FileResponse(UUID uuid, String name, byte[] data, int size, FileMessage.FileType type) {
		this.uuid = uuid;
		this.name = name;
		this.data = data;
		this.size = size;
		this.type = type;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public byte[] getData() {
		return data;
	}

	public int getSize() {
		return size;
	}

	public FileMessage.FileType getType() {
		return type;
	}
}
