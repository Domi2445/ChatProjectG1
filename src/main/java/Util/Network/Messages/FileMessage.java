package Util.Network.Messages;

import User.Model.User;

import java.io.Serial;
import java.util.UUID;

public class FileMessage extends Message {
	@Serial
	private static final long serialVersionUID = 1L;

	private final byte[] content;
	private final UUID uuid;
    private final String fileName;
    private final FileType fileType;

    public FileMessage(User sender, byte[] content, UUID uuid, String fileName, FileType fileType) {
        super(sender);
        this.content = content;
		this.uuid = uuid;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public byte[] getContent() {
        return content;
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
