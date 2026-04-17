package Util.Network.Messages;

import Util.User;

import java.io.Serial;

public class FileMessage extends Message {
	@Serial
	private static final long serialVersionUID = 1L;

	private final byte[] content;
    private final String fileName;
    private final FileType fileType;

    public FileMessage(User sender, byte[] content, String fileName, FileType fileType) {
        super(sender);
        this.content = content;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public byte[] getContent() {
        return content;
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
