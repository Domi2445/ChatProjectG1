package Server;

import Util.Message.Message;
import Util.SocketProxy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MessageBroker implements Runnable {
	private final BlockingQueue<Message> messageBrokerQueue;
	private final List<SocketProxy> clients;

	public MessageBroker(BlockingQueue<Message> messageBrokerQueue, List<SocketProxy> clients) {
		this.messageBrokerQueue = messageBrokerQueue;
		this.clients = clients;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Message message = messageBrokerQueue.take();

				for (SocketProxy client : clients) {
					try {
						client.out.writeObject(message);
						client.out.flush();
					} catch (IOException e) {
						System.out.println("Fehler beim Senden:\n" + e);
					}
				}

			} catch (InterruptedException e) {
				break;
			}
		}
	}
}