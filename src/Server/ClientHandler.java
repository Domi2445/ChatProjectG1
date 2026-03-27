package Server;

import Util.SocketProxy;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
	private final SocketProxy client;
	private final BlockingQueue<String> messageBrokerQueue;

	public ClientHandler(SocketProxy client, BlockingQueue<String> messageBrokerQueue) throws IOException {
		this.client = client;
		this.messageBrokerQueue = messageBrokerQueue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				String message = client.in.readLine();
				IO.println("Received message from client: " + message);
				messageBrokerQueue.put(message);

			} catch (IOException e) {
				IO.println("Failed to read line:\n" + e);
				break;

			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
