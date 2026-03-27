package Server;

import Util.Message;
import Util.SocketProxy;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
	private final SocketProxy client;
	private final BlockingQueue<Message> messageBrokerQueue;

	public ClientHandler(SocketProxy client, BlockingQueue<Message> messageBrokerQueue) throws IOException {
		this.client = client;
		this.messageBrokerQueue = messageBrokerQueue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				String raw = client.in.readLine();
				if (raw == null) break;
				Message message = Message.fromString(raw);
				System.out.println("Empfangen von " + message.getSender() + ": " + message.getContent());
				messageBrokerQueue.put(message);

			} catch (IOException e) {
				System.out.println("Verbindung getrennt:\n" + e);
				break;
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}