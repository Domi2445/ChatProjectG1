package Server;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AudioRelayServer implements Runnable {
	private final int port;

	// roomId
	private final Map<String, List<InetSocketAddress>> rooms = new ConcurrentHashMap<>();

	public AudioRelayServer(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		try (DatagramSocket socket = new DatagramSocket(port)) {
			System.out.println("AudioRelayServer running on port " + port);
			byte[] buffer = new byte[2048];

			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				InetSocketAddress sender = new InetSocketAddress(
					packet.getAddress(), packet.getPort()
				);

				String raw = new String(packet.getData(), 0, packet.getLength());

				if (raw.startsWith("JOIN:")) {
					// Client registriert sich in einem Raum:
					String roomId = raw.substring(5).trim();
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
