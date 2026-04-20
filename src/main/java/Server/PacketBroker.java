package Server;

import User.Model.User;
import Util.Network.Notifications.LeaveNotification;
import Util.Network.Packet;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/// Verteilt Pakete an alle angemeldeten Clients.
public class PacketBroker implements Runnable {
	public static final int MAX_INCOMING_PACKETS = 128;
	public static final int MAX_OUTGOING_PACKETS = 128;
	public static final int MAX_CLIENTS = 16;

	/// Queue für Pakete, die an alle verbundenen Clients gesendet werden sollen.
	private final BlockingQueue<Packet> broadcastPacketQueue;
	/// Liste aller aktuell verbundenen Clients.
	private final List<ClientProxy> clients;
	private final AtomicBoolean running;

	public PacketBroker() {
		this.broadcastPacketQueue = new ArrayBlockingQueue<>(MAX_INCOMING_PACKETS);
		this.clients = new ArrayList<>(MAX_CLIENTS);
		this.running = new AtomicBoolean(true);
	}

	@Override
	public void run() {
		ArrayList<ClientProxy> clientsToRemove = new ArrayList<>();

		while (running.get() && !Thread.currentThread().isInterrupted()) {
			try {
				Packet packet = broadcastPacketQueue.take();

				synchronized (clients) {
					for (var client : clients) {
						if (client.getSocket().isClosed() || client.getStopFlag()) {
							clientsToRemove.add(client);
							continue;
						}

						if (!client.tryEnqueuePacket(packet)) {
							System.err.println("Client outPacketQueue ist voll");
							clientsToRemove.add(client);
						}
					}

					clients.removeAll(clientsToRemove);
				}

				for (var client : clientsToRemove) {
					try {
						client.close();
					} catch (IOException e) {
						System.err.println("Fehler beim Schließen eines Clients: " + e);
					}

					User user = client.getUser();

					// todo: Benutzernamen des Clients übergeben oder keine Benachrichtigung senden wenn nicht eingeloggt
					if (user == null) {
						user = new User();
						user.setUsername("Platzhalter");
					}

					// todo: Sende LeaveNotification direkt nach dem Disconnect
					broadcast(new LeaveNotification(user));
				}

				clientsToRemove.clear();

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		closeAllClients();
	}

	public boolean register(SocketProxy socket) {
		if (!running.get()) {
			return false;
		}

		if (clients.size() >= MAX_CLIENTS) {
			return false;
		}

		BlockingQueue<Packet> outPacketQueue = new ArrayBlockingQueue<>(MAX_OUTGOING_PACKETS);
		var client = new ClientProxy(socket, broadcastPacketQueue, outPacketQueue, Server.THREAD_EXECUTOR);

		synchronized (clients) {
			clients.add(client);
		}

		return true;
	}

	public void broadcast(Packet packet) throws InterruptedException {
		if (!running.get()) {
			return;
		}

		broadcastPacketQueue.put(packet);
	}

	public void shutdown() {
		running.set(false);
	}

	private void closeAllClients() {
		ArrayList<ClientProxy> clientsToClose;

		synchronized (clients) {
			clientsToClose = new ArrayList<>(clients);
			clients.clear();
		}

		for (var client : clientsToClose) {
			try {
				client.close();
			} catch (IOException e) {
				System.err.println("Fehler beim Schließen eines Clients: " + e);
			}
		}
	}
}
