package Server;

import Util.Network.Packet;
import Util.Network.SocketProxy;

public class PacketCarrier
{
	public final Packet packet;
	public final SocketProxy sender;

	public PacketCarrier(Packet packet, SocketProxy sender)
	{
		this.packet = packet;
		this.sender = sender;
	}
}
