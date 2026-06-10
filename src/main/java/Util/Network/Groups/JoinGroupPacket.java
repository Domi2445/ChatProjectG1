package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;
import java.util.UUID;

// send this packet to the server to join an existing group.
// usage: outPacketQueue.put(new JoinGroupPacket(group.getId()));
// you need the group's uuid — get it from a Group object via group.getId().
// once joined, you will receive all messages sent to that group.
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
