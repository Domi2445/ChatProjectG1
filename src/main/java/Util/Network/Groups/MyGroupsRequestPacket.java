package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;

// send this to the server to get the list of groups you are currently a member of.
// usage: outPacketQueue.put(new MyGroupsRequestPacket());
// the server responds with a GroupListResponsePacket containing only your groups.
public class MyGroupsRequestPacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;
}
