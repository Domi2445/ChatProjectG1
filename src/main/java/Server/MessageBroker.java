package Server;

import Util.SocketProxy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MessageBroker implements Runnable {
	private final BlockingQueue<String> messageBrokerQueue;
	private final List<SocketProxy> clients;

	public MessageBroker(BlockingQueue<String> messageBrokerQueue, List<SocketProxy> clients) {
		this.messageBrokerQueue = messageBrokerQueue;
		this.clients = clients;
	}

	@Override
	public void run() {
		while (true) {
			try {
				String message = messageBrokerQueue.take();

				for (SocketProxy client : clients) {
					try {
						client.out.write(message + '\n');
						client.out.flush();

					} catch (IOException e) {
						System.out.println("Failed to send message to client:\n" + e);
					}
				}

			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
