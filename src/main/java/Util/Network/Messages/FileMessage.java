package Util.Network.Messages;

import User.Model.User;

import java.io.Serial;

public class FileMessage extends Message {
	@Serial
	private static final long serialVersionUID = 2L;

	private final byte[] content;
	private final String fileExtension;

	public FileMessage(User sender, byte[] content, String fileExtension) {
		super(sender);
		this.content = content;
		this.fileExtension = fileExtension;
	}

	public byte[] getContent() {
		return content;
	}

	public String getFileExtension() {
		return fileExtension;
	}
}
