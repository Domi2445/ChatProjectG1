package Server;

import Util.Network.Notifications.LeaveNotification;
import Util.Network.Packet;
import Util.SocketProxy;
import Util.User;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
	private final SocketProxy client;
	private final BlockingQueue<Packet> packetBrokerQueue;

	public ClientHandler(SocketProxy client, BlockingQueue<Packet> packetBrokerQueue) throws IOException {
		this.client = client;
		this.packetBrokerQueue = packetBrokerQueue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Packet packet = (Packet) client.in.readObject();
				packetBrokerQueue.put(packet);
			} catch (IOException e) {
				System.out.println("Verbindung getrennt: " + e);
				client.setStopFlag();

				try {
					// todo: Sobald es ein Loginsystem gibt, hier den Benutzernamen des verbindenden Clients übergeben
					packetBrokerQueue.put(new LeaveNotification(new User("Platzhalter")));
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}

				break;
			} catch (ClassNotFoundException e) {
				System.out.println("Ungültiges Paket empfangen: " + e);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
