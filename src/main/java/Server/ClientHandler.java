package Server;

import Util.Message.Message;
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
				Message message = (Message) client.in.readObject();
				System.out.println("Empfangen von " + message.getSender().getIdentifier() + ": " + message.getContent());
				messageBrokerQueue.put(message);
			} catch (IOException e) {
				System.out.println("Verbindung getrennt:\n" + e);
				break;
			} catch (ClassNotFoundException | InterruptedException e) {
				break;
			}
		}
	}
}
