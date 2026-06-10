package Util.Network.Groups;

import Util.Network.Packet;
import java.io.Serial;

// send this packet to the server to request the current list of all groups.
// usage: outPacketQueue.put(new GroupListRequestPacket());
// the server responds with a GroupListResponsePacket containing all groups.
public class GroupListRequestPacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;
}
