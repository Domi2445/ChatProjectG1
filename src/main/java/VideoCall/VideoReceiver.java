package VideoCall;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VideoReceiver implements Runnable {
	private final int port;
	private final ImageView display;
	private volatile boolean running = false;

	public VideoReceiver(int port, ImageView display) {
		this.port = port;
		this.display = display;
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
			DatagramSocket socket = new DatagramSocket(port);
			byte[] buffer = new byte[65507];
			System.out.println("VideoReceiver lauscht auf Port " + port);

			while (running) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

				Image image = new Image(new ByteArrayInputStream(data));
				Platform.runLater(() -> display.setImage(image));
			}

			socket.close();
		} catch (Exception e) {
			if (running) e.printStackTrace();
		}
	}
}
