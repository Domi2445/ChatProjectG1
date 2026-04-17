package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;

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
