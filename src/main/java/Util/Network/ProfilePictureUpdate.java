package Util.Network;

import java.io.Serial;
import java.util.Arrays;

public class ProfilePictureUpdate extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String username;
	private final byte[] imageBytes;
	private final String contentType;

	public ProfilePictureUpdate(String username, byte[] imageBytes, String contentType) {
		this.username = username;
		this.imageBytes = imageBytes == null ? null : Arrays.copyOf(imageBytes, imageBytes.length);
		this.contentType = contentType;
	}

	public String getUsername() {
		return username;
	}

	public byte[] getImageBytes() {
		return imageBytes == null ? null : Arrays.copyOf(imageBytes, imageBytes.length);
	}

	public String getContentType() {
		return contentType;
	}
}
