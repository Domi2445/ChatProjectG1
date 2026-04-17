package Util.Network.Messages;

import Util.User;

import java.io.Serial;
import java.util.UUID;

public class FileMessage extends Message {
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID uuid;
    private final String fileName;
    private final FileType fileType;

    public FileMessage(User sender, UUID uuid, String fileName, FileType fileType) {
        super(sender);
		this.uuid = uuid;
        this.fileName = fileName;
        this.fileType = fileType;
    }

	public UUID getUuid() {
		return uuid;
	}

	public String getFileName() {
        return fileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public enum FileType {
        FILE,
        IMAGE,
    }
}
