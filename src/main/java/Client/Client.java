package Client;

import Util.Message.Message;
import Util.SocketProxy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

public class Client implements Runnable {
	private final BlockingQueue<Message> outgoingMessageQueue;
	private final BlockingQueue<Message> incomingMessageQueue;
	private final SocketProxy socket;

	public Client(String ip, int port, BlockingQueue<Message> outgoingMessageQueue, BlockingQueue<Message> incomingMessageQueue) throws IOException {
		this.socket = new SocketProxy(new Socket(ip, port));
		this.socket.socket.setSoTimeout(100);
		this.outgoingMessageQueue = outgoingMessageQueue;
		this.incomingMessageQueue = incomingMessageQueue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Message message = (Message) socket.in.readObject();
				System.out.println("Nachricht empfangen");
				incomingMessageQueue.put(message);
			} catch (SocketTimeoutException ignored) {
			} catch (IOException e) {
				System.out.println("Verbindung zum Server getrennt:\n" + e);
				break;
			} catch (ClassNotFoundException | InterruptedException e) {
				throw new RuntimeException(e);
			}

			Message message = outgoingMessageQueue.poll();
			if (message != null) {
				try {
					socket.out.writeObject(message);
					socket.out.flush();
					System.out.println("Nachricht gesendet");
				} catch (IOException e) {
					System.out.println("Fehler beim Senden:\n" + e);
					break;
				}
			}
		}
	}
}
