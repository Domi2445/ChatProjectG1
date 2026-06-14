package Server;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VideoRelayServer implements Runnable {
	private final int port;
	private final Map<String, List<InetSocketAddress>> rooms = new ConcurrentHashMap<>();

	private static final byte[] VJOIN_PREFIX = "VJOIN:".getBytes(StandardCharsets.US_ASCII);

	public VideoRelayServer(int port) {
		this.port = port;
	}

	private boolean isVjoin(DatagramPacket packet) {
		if (packet.getLength() < VJOIN_PREFIX.length) return false;
		byte[] data = packet.getData();
		for (int i = 0; i < VJOIN_PREFIX.length; i++) {
			if (data[i] != VJOIN_PREFIX[i]) return false;
		}
		return true;
	}

	@Override
	public void run() {
		try (DatagramSocket socket = new DatagramSocket(port)) {
			System.out.println("VideoRelayServer running on port " + port);
			byte[] buffer = new byte[65507];

			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				InetSocketAddress sender = new InetSocketAddress(
					packet.getAddress(), packet.getPort()
				);

				if (isVjoin(packet)) {
					String roomId = new String(packet.getData(), VJOIN_PREFIX.length,
						packet.getLength() - VJOIN_PREFIX.length, StandardCharsets.UTF_8).trim();
					rooms.computeIfAbsent(roomId, k -> new ArrayList<>());
					if (!rooms.get(roomId).contains(sender)) {
						rooms.get(roomId).add(sender);
						System.out.println(sender + " joined video room: " + roomId);
					}
				} else {
					byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
					for (List<InetSocketAddress> members : rooms.values()) {
						if (members.contains(sender)) {
							for (InetSocketAddress member : members) {
								if (!member.equals(sender)) {
									socket.send(new DatagramPacket(
										data, data.length,
										member.getAddress(), member.getPort()
									));
								}
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
