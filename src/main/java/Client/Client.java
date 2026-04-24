package Client;

import Util.Network.Packet;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

public class Client implements Runnable {
	private final BlockingQueue<Packet> out;
	private final BlockingQueue<Packet> in;
	private final SocketProxy socket;

	public Client(String ip, int port, BlockingQueue<Packet> out, BlockingQueue<Packet> in) throws IOException {
		Socket socket = new Socket(ip, port);
		this.socket = new SocketProxy(socket);
		socket.setSoTimeout(100);
		this.out = out;
		this.in = in;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Packet packet = (Packet) socket.getInputStream().readObject();
				in.put(packet);
			} catch (SocketTimeoutException ignored) {
			} catch (IOException e) {
				// todo: Anzeige in der GUI, dass die Verbindung zum Server getrennt wurde
				System.err.println("Verbindung zum Server getrennt:\n" + e);
				break;
			} catch (ClassNotFoundException | InterruptedException e) {
				throw new RuntimeException(e);
			}

			Packet packet = out.poll();
			if (packet != null) {
				try {
					socket.getOutputStream().writeObject(packet);
					socket.getOutputStream().flush();
				} catch (IOException e) {
					System.err.println("Fehler beim Senden:\n" + e);
					break;
				}
			}
		}
	}
}
