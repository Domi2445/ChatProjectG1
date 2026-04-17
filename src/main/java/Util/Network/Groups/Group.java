package Util.Network.Groups;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class Group implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID id;
	private final String name;
	private final String creatorUsername;

	public Group(UUID id, String name, String creatorUsername) {
		this.id = id;
		this.name = name;
		this.creatorUsername = creatorUsername;
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public String getCreatorUsername() { return creatorUsername; }

	@Override
	public String toString() { return name; }
}
