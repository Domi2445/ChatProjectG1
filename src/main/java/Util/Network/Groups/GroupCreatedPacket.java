package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;

public class GroupCreatedPacket extends Packet {
	@Serial
	private static final long serialVersionUID = 1L;

	private final Group group;

	public GroupCreatedPacket(Group group) {
		this.group = group;
	}

	public Group getGroup() { return group; }
}
