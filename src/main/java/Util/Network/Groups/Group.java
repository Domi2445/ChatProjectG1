package Util.Network.Groups;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

// represents a group in the chat. this object is sent over the network so it must stay serializable.
// the id is a uuid that uniquely identifies the group — use this id when sending group messages or joining/leaving.
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
