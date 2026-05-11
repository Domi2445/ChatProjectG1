package VideoCall;

import com.github.sarxos.webcam.Webcam;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VideoSender implements Runnable {
	private final String relayIp;
	private final int relayPort;
	private final String roomId;
	private volatile boolean running = false;

	public VideoSender(String relayIp, int relayPort, String roomId) {
		this.relayIp = relayIp;
		this.relayPort = relayPort;
		this.roomId = roomId;
	}

	public void start() {
		running = true;
		new Thread(this).start();
	}

	public void stop() {
		running = false;
	}

	@Override
	public void run() {
		try {
			Webcam webcam = Webcam.getDefault();
			if (webcam == null) {
				System.out.println("Keine Webcam gefunden!");
				return;
			}
			webcam.open();

			DatagramSocket socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName(relayIp);

			// Join room
			byte[] joinMsg = ("VJOIN:" + roomId).getBytes();
			socket.send(new DatagramPacket(joinMsg, joinMsg.length, address, relayPort));

			System.out.println("VideoSender gestartet");

			while (running) {
				BufferedImage frame = webcam.getImage();
				if (frame == null) continue;

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(frame, "jpg", baos);
				byte[] data = baos.toByteArray();

				// Send in chunks of 60KB
				int chunkSize = 60000;
				int totalChunks = (int) Math.ceil((double) data.length / chunkSize);

				for (int i = 0; i < totalChunks; i++) {
					int start = i * chunkSize;
					int end = Math.min(start + chunkSize, data.length);
					byte[] chunk = new byte[end - start];
					System.arraycopy(data, start, chunk, 0, chunk.length);

					socket.send(new DatagramPacket(chunk, chunk.length, address, relayPort));
				}

				Thread.sleep(33); // ~30fps
			}

			webcam.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
