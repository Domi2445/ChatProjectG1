package Server;

import Util.Network.Packet;

public record IncomingPacket(Packet packet, ClientProxy sender) {}
