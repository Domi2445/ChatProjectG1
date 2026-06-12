package Server;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AudioRelayServer implements Runnable {
	private final int port;

	// roomId
	private final Map<String, List<InetSocketAddress>> rooms = new ConcurrentHashMap<>();

	private static final byte[] JOIN_PREFIX = "JOIN:".getBytes(StandardCharsets.US_ASCII);

	public AudioRelayServer(int port) {
		this.port = port;
	}

	private boolean isJoin(DatagramPacket packet) {
		if (packet.getLength() < JOIN_PREFIX.length) return false;
		byte[] data = packet.getData();
		for (int i = 0; i < JOIN_PREFIX.length; i++) {
			if (data[i] != JOIN_PREFIX[i]) return false;
		}
		return true;
	}

	@Override
	public void run() {
		try (DatagramSocket socket = new DatagramSocket(port)) {
			System.out.println("AudioRelayServer running on port " + port);
			byte[] buffer = new byte[8192];

			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				InetSocketAddress sender = new InetSocketAddress(
					packet.getAddress(), packet.getPort()
				);

				if (isJoin(packet)) {
					// Client registriert sich in einem Raum:
					String roomId = new String(packet.getData(), JOIN_PREFIX.length,
						packet.getLength() - JOIN_PREFIX.length, StandardCharsets.UTF_8).trim();
					rooms.computeIfAbsent(roomId, k -> new ArrayList<>());
					if (!rooms.get(roomId).contains(sender)) {
						rooms.get(roomId).add(sender);
						System.out.println(sender + " joined room: " + roomId);
					}

				} else {
					// Audiodaten weiterleiten — nur an Mitglieder desselben Raums
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
