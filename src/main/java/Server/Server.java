package Server;

import Util.SocketProxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server implements Runnable {
	private final ServerSocket server;

	private final BlockingQueue<String> messageBrokerQueue;
	private final List<SocketProxy> clients;

	public Server(int port) throws IOException {
		server = new ServerSocket(port);
		messageBrokerQueue = new ArrayBlockingQueue<>(32);
		clients = Collections.synchronizedList(new ArrayList<>());
	}

	@Override
	public void run() {
		new Thread(new MessageBroker(messageBrokerQueue, clients)).start();

		while (true) {
			try {
				Socket socket = server.accept();
				SocketProxy client = new SocketProxy(socket);
				System.out.println("Accepted connection");

				clients.add(client);

				new Thread(new ClientHandler(client, messageBrokerQueue)).start();

			} catch (IOException e) {
				System.out.println("Failed to accept connection:\n" + e);
				break;
			}
		}
	}
}
