package Client;

import Util.SocketProxy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

public class Client implements Runnable {
	private final BlockingQueue<String> outgoingMessageQueue;
	private final BlockingQueue<String> incomingMessageQueue;
	private final SocketProxy socket;

	public Client(String ip, int port, BlockingQueue<String> outgoingMessageQueue, BlockingQueue<String> incomingMessageQueue) throws IOException {
		this.socket = new SocketProxy(new Socket(ip, port));
		this.socket.socket.setSoTimeout(100);

		this.outgoingMessageQueue = outgoingMessageQueue;
		this.incomingMessageQueue = incomingMessageQueue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				String message = socket.in.readLine();
				if (message != null) {
					System.out.println("Received message from server: " + message);
					incomingMessageQueue.put(message);
				}
			} catch (SocketTimeoutException ignored) {
			} catch (IOException e) {
				System.out.println("Failed to read line from server:\n" + e);
				break;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			String message = outgoingMessageQueue.poll();
			if (message != null) {
				try {
					socket.out.write(message + '\n');
					socket.out.flush();
					System.out.println("Sent message to server");

				} catch (IOException e) {
					System.out.println("Failed to send message to server:\n" + e);
					break;
				}
			}
		}
	}
}
