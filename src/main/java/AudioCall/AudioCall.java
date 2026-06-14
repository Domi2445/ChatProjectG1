package AudioCall;

import javax.sound.sampled.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class AudioCall {
	private volatile boolean running = false;
	private DatagramSocket socket;

	public void start(String relayIp, int relayPort, String roomId) throws Exception {
		running = true;

		socket = new DatagramSocket();
		InetAddress relayAddress = InetAddress.getByName(relayIp);

		// Raum beim Relay registrieren
		byte[] joinMsg = ("JOIN:" + roomId).getBytes(StandardCharsets.UTF_8);
		socket.send(new DatagramPacket(joinMsg, joinMsg.length, relayAddress, relayPort));

		// Thread 1: Mikrofon → Relay
		new Thread(() -> {
			TargetDataLine mic = null;
			try {
				AudioFormat format = findSupportedFormat(TargetDataLine.class);
				if (format == null) {
					System.out.println("Kein unterstütztes Mikrofonformat gefunden!");
					return;
				}
				System.out.println("Mikrofonformat: " + format.getSampleRate() + " Hz");

				// 20 ms pro Chunk – kleine Puffer = weniger Latenz = weniger Echo
				int chunkBytes = chunkBytes(format);

				mic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
				mic.open(format, chunkBytes * 2); // 40 ms interner Puffer
				mic.start();

				byte[] buffer = new byte[chunkBytes];
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
				AudioFormat format = findSupportedFormat(SourceDataLine.class);
				if (format == null) {
					System.out.println("Kein unterstütztes Lautsprecherformat gefunden!");
					return;
				}
				System.out.println("Lautsprecherformat: " + format.getSampleRate() + " Hz");

				int chunkBytes = chunkBytes(format);

				speakers = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
				// 60 ms Jitter-Puffer – guter Kompromiss zwischen Latenz und Stabilität
				int bufferSize = chunkBytes * 3;
				speakers.open(format, bufferSize);
				speakers.start();

				byte[] buffer = new byte[chunkBytes];
				while (running) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					// Latenz begrenzen: Kommen Pakete schneller an, als sie abgespielt werden,
					// staut sich der Puffer auf und die Verzögerung wächst über den Anruf hinweg.
					// Übersteigt der Rückstand ~40 ms, verwerfen wir ihn, damit die Latenz konstant bleibt.
					if (bufferSize - speakers.available() > chunkBytes * 2) {
						speakers.flush();
					}
					speakers.write(packet.getData(), 0, packet.getLength());
				}
			} catch (Exception e) {
				if (running) e.printStackTrace();
			} finally {
				if (speakers != null) { speakers.flush(); speakers.stop(); speakers.close(); }
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

	/** Gibt das erste unterstützte Audioformat zurück (16 kHz bevorzugt). */
	private static <T extends DataLine> AudioFormat findSupportedFormat(Class<T> lineClass) {
		int[] sampleRates = {16000, 22050, 44100, 48000};
		for (int rate : sampleRates) {
			AudioFormat f = new AudioFormat(rate, 16, 1, true, false);
			if (AudioSystem.isLineSupported(new DataLine.Info(lineClass, f))) return f;
		}
		return null;
	}

	/** Berechnet einen 20-ms-Chunk in Bytes für das gegebene Format. */
	private static int chunkBytes(AudioFormat format) {
		// sampleRate * frameSize * 20ms / 1000ms
		return Math.max(64, (int)(format.getSampleRate() * format.getFrameSize() * 0.02f));
	}
}
