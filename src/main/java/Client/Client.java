package Client;

import Util.Message;
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
				String raw = socket.in.readLine();
				if (raw != null) {
					Message message = Message.fromString(raw);
					System.out.println("Empfangen: " + message.getContent());
					incomingMessageQueue.put(message);
				}
			} catch (SocketTimeoutException ignored) {
			} catch (IOException e) {
				System.out.println("Verbindung zum Server getrennt:\n" + e);
				break;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			Message message = outgoingMessageQueue.poll();
			if (message != null) {
				try {
					socket.out.write(message.toString() + '\n');
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