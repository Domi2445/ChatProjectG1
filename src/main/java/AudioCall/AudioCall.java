package AudioCall;

import javax.sound.sampled.*;
import java.net.*;

public class AudioCall {
	private volatile boolean running = false;
	private DatagramSocket socket;

	public void start(String relayIp, int relayPort, String roomId) throws Exception {
		running = true;

		socket = new DatagramSocket();
		InetAddress relayAddress = InetAddress.getByName(relayIp);

		// Raum beim Relay registrieren
		byte[] joinMsg = ("JOIN:" + roomId).getBytes();
		socket.send(new DatagramPacket(joinMsg, joinMsg.length, relayAddress, relayPort));

		// Thread 1: Mikrofon → Relay
		new Thread(() -> {
			TargetDataLine mic = null;
			try {
				AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
				mic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
				mic.open(format);
				mic.start();
				System.out.println("JOIN отправлен: " + relayIp + ":" + relayPort + " room: " + roomId);

				byte[] buffer = new byte[1024];
				while (running) {
					int bytesRead = mic.read(buffer, 0, buffer.length);
					if (bytesRead > 0)
						socket.send(new DatagramPacket(buffer, bytesRead, relayAddress, relayPort));
				}
			} catch (Exception e) {
				if (running) e.printStackTrace();
			} finally {
				if (mic != null) { mic.stop(); mic.close(); }
			}
		}).start();

		// Thread 2: Relay → Lautsprecher
		new Thread(() -> {
			SourceDataLine speakers = null;
			try {
				AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
				speakers = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
				speakers.open(format);
				speakers.start();

				byte[] buffer = new byte[2048];
				while (running) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					System.out.println("Paket empfangen: " + packet.getLength() + " bytes");
					speakers.write(packet.getData(), 0, packet.getLength());
				}
			} catch (Exception e) {
				if (running) e.printStackTrace();
			} finally {
				if (speakers != null) { speakers.stop(); speakers.close(); }
				if (socket != null && !socket.isClosed()) socket.close();
			}
		}).start();
	}

	public void stop() {
		running = false;
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}
}
