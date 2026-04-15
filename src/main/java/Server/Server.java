package Server;

import Util.Network.Notifications.JoinNotification;
import Util.Network.Packet;
import Util.SocketProxy;
import Util.User;

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
	private final BlockingQueue<Packet> packetBrokerQueue;
	private final List<SocketProxy> clients;

	public Server(int port) throws IOException {
		server = new ServerSocket(port);
		packetBrokerQueue = new ArrayBlockingQueue<>(32);
		clients = Collections.synchronizedList(new ArrayList<>());
	}

	@Override
	public void run() {
		new Thread(new PacketBroker(packetBrokerQueue, clients)).start();

		while (true) {
			try {
				Socket socket = server.accept();
				SocketProxy client = new SocketProxy(socket);
				System.out.println("Verbindung akzeptiert");

				clients.add(client);

				// todo: Sobald es ein Loginsystem gibt, hier den Benutzernamen des verbindenden Clients übergeben
				packetBrokerQueue.put(new JoinNotification(new User("Platzhalter")));

				new Thread(new ClientHandler(client, packetBrokerQueue)).start();

			} catch (IOException e) {
				System.out.println("Fehler beim Verbindungsaufbau:\n" + e);
				break;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
