package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;

// send this packet to the server to create a new group.
// usage: outPacketQueue.put(new CreateGroupPacket("group name"));
// the server will create the group and automatically add the sender as the first member.
public class CreateGroupPacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final String groupName;

	public CreateGroupPacket(String groupName)
	{
		this.groupName = groupName;
	}

	public String getGroupName() { return groupName; }
}
