package Server;

import Util.Network.Packet;

public class PacketCarrier
{
	public final Packet packet;
	public final ClientProxy sender; // null = server-generated

	public PacketCarrier(Packet packet, ClientProxy sender)
	{
		this.packet = packet;
		this.sender = sender;
	}
}
