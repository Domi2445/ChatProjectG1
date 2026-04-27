package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;
import java.util.UUID;

// send this packet to the server to leave a group you are currently in.
// usage: outPacketQueue.put(new LeaveGroupPacket(group.getId()));
// after leaving you will no longer receive messages sent to that group.
public class LeaveGroupPacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID groupId;

	public LeaveGroupPacket(UUID groupId)
	{
		this.groupId = groupId;
	}

	public UUID getGroupId() { return groupId; }
}
