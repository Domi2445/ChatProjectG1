package VideoCall;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VideoCall {
	private volatile boolean running = false;
	private DatagramSocket socket;
	private ImageView display;

	private static final int HEADER = 8;
	private static final int CHUNK = 60000;

	public void start(String relayIp, int relayPort, String roomId, ImageView display) throws Exception {
		this.display = display;
		running = true;
		socket = new DatagramSocket();
		InetAddress relay = InetAddress.getByName(relayIp);

		byte[] join = ("VJOIN:" + roomId).getBytes(StandardCharsets.UTF_8);
		socket.send(new DatagramPacket(join, join.length, relay, relayPort));

		new Thread(() -> {
			try {
				Webcam webcam = Webcam.getDefault();
				if (webcam == null) { System.out.println("Keine Webcam"); return; }
				webcam.open();
				int frameId = 0;
				while (running) {
					BufferedImage img = webcam.getImage();
					if (img == null) continue;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(img, "jpg", baos);
					byte[] data = baos.toByteArray();
					int total = (int) Math.ceil((double) data.length / CHUNK);
					for (int i = 0; i < total; i++) {
						int s = i * CHUNK, e = Math.min(s + CHUNK, data.length);
						ByteBuffer buf = ByteBuffer.allocate(HEADER + (e - s));
						buf.putInt(frameId).putShort((short) i).putShort((short) total);
						buf.put(data, s, e - s);
						byte[] pkt = buf.array();
						socket.send(new DatagramPacket(pkt, pkt.length, relay, relayPort));
					}
					frameId++;
					Thread.sleep(33);
				}
				webcam.close();
			} catch (Exception ex) { if (running) ex.printStackTrace(); }
		}, "VideoSender").start();

		new Thread(() -> {
			byte[] buffer = new byte[65507];
			Map<Integer, byte[][]> frames = new HashMap<>();
			Map<Integer, Integer> received = new HashMap<>();
			try {
				while (running) {
					DatagramPacket p = new DatagramPacket(buffer, buffer.length);
					socket.receive(p);
					if (p.getLength() < HEADER) continue;
					ByteBuffer buf = ByteBuffer.wrap(p.getData(), 0, p.getLength());
					int id = buf.getInt();
					int idx = buf.getShort();
					int total = buf.getShort();
					if (total <= 0 || idx < 0 || idx >= total) continue;
					byte[] payload = new byte[p.getLength() - HEADER];
					buf.get(payload);
					frames.computeIfAbsent(id, k -> new byte[total][]);
					byte[][] chunks = frames.get(id);
					if (chunks[idx] == null) {
						chunks[idx] = payload;
						int got = received.merge(id, 1, Integer::sum);
						if (got == total) {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							for (byte[] c : chunks) out.write(c);
							Image image = new Image(new ByteArrayInputStream(out.toByteArray()));
							Platform.runLater(() -> display.setImage(image));
							frames.remove(id);
							received.remove(id);
						}
					}
				}
			} catch (Exception ex) { if (running) ex.printStackTrace(); }
		}, "VideoReceiver").start();
	}

	public void stop() {
		running = false;
		if (socket != null && !socket.isClosed()) socket.close();
		// Letztes Frame löschen, damit kein eingefriertes Bild bleibt
		if (display != null) {
			Platform.runLater(() -> display.setImage(null));
			display = null;
		}
	}
}
