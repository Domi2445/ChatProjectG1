package Server;

import Util.Network.Packet;
import Util.SocketProxy;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
	private final SocketProxy client;
	private final BlockingQueue<Packet> messageBrokerQueue;

	public ClientHandler(SocketProxy client, BlockingQueue<Packet> messageBrokerQueue) throws IOException {
		this.client = client;
		this.messageBrokerQueue = messageBrokerQueue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Packet packet = (Packet) client.in.readObject();
				messageBrokerQueue.put(packet);
			} catch (IOException e) {
				System.out.println("Verbindung getrennt:\n" + e);
				break;
			} catch (ClassNotFoundException | InterruptedException e) {
				break;
			}
		}
	}
}
