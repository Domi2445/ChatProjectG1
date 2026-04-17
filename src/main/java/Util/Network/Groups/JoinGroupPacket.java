package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;
import java.util.UUID;

public class JoinGroupPacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID groupId;

	public JoinGroupPacket(UUID groupId)
	{
		this.groupId = groupId;
	}

	public UUID getGroupId() { return groupId; }
}
