package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;
import java.util.Collection;
import java.util.List;

// sent by the server in response to GroupListRequestPacket or MyGroupsRequestPacket.
// call getGroups() to get the list and display them in the ui.
public class GroupListResponsePacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final List<Group> groups;

	public GroupListResponsePacket(Collection<Group> groups)
	{
		this.groups = List.copyOf(groups);
	}

	public List<Group> getGroups() { return groups; }
}
