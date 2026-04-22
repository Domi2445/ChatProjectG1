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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/// Verteilt Pakete an alle angemeldeten Clients.
public class PacketBroker implements Runnable {
	public static final int MAX_INCOMING_PACKETS = 128;
	public static final int MAX_OUTGOING_PACKETS = 128;
	public static final int MAX_CLIENTS = 16;

	private final ExecutorService threadExecutor;

	/// Queue für Pakete, die an alle verbundenen Clients gesendet werden sollen.
	private final BlockingQueue<Packet> broadcastPacketQueue;
	/// Liste aller aktuell verbundenen Clients.
	private final List<ClientProxy> clients;
	private final AtomicBoolean stopFlag;

	public PacketBroker(ExecutorService threadExecutor) {
		this.threadExecutor = threadExecutor;

		this.broadcastPacketQueue = new ArrayBlockingQueue<>(MAX_INCOMING_PACKETS);
		this.clients = new ArrayList<>(MAX_CLIENTS);
		this.stopFlag = new AtomicBoolean(false);
	}

	@Override
	public void run() {
		ArrayList<ClientProxy> clientsToUnregister = new ArrayList<>();

		while (!stopFlag.get() && !Thread.currentThread().isInterrupted()) {
			try {
				Packet packet = broadcastPacketQueue.take();

				synchronized (clients) {
					for (var client : clients) {
						if (client.shouldStop()) {
							clientsToUnregister.add(client);
							continue;
						}

						if (!client.tryEnqueuePacket(packet)) {
							System.err.println("Client outPacketQueue ist voll");
							clientsToUnregister.add(client);
						}
					}
				}

				for (var client : clientsToUnregister) {
					boolean result = unregister(client);

					if (!result) {
						System.err.println("Zu entfernenden Client nicht gefunden");
						continue;
					}

					User user = client.getUser();

					// todo: Benutzernamen des Clients übergeben oder keine Benachrichtigung senden wenn nicht eingeloggt
					if (user == null) {
						user = new User();
						user.setUsername("Platzhalter");
					}

					// todo: Sende LeaveNotification direkt nach dem Disconnect
					if (!broadcast(new LeaveNotification(user))) {
						System.err.println("broadcastPacketQueue ist voll, Paket wurde verworfen");
					}
				}

				clientsToUnregister.clear();

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		closeAllClients();
	}

	/// Fügt einen neuen Client zur Liste der verbundenen Clients hinzu.
	/// Gibt `true` zurück, wenn der Client erfolgreich registriert wurde.
	public boolean register(SocketProxy socket) {
		if (stopFlag.get()) {
			return false;
		}

		ArrayList<ClientProxy> clientsToUnregister = new ArrayList<>();

		synchronized (clients) {
			if (clients.size() >= MAX_CLIENTS) {
				for (var client : clients) {
					if (client.shouldStop()) {
						clientsToUnregister.add(client);
					}
				}

				if (clientsToUnregister.isEmpty()) {
					return false;
				}
			}
		}

		for (var client : clientsToUnregister) {
			unregister(client);
		}

		BlockingQueue<Packet> outPacketQueue = new ArrayBlockingQueue<>(MAX_OUTGOING_PACKETS);
		var client = new ClientProxy(socket, broadcastPacketQueue, outPacketQueue, threadExecutor);

		synchronized (clients) {
			clients.add(client);
		}

		return true;
	}

	/// Entfernt einen Client aus der Liste der verbundenen Clients und schließt die Verbindung.
	/// Gibt `true` zurück, wenn der Client erfolgreich entfernt wurde.
	public boolean unregister(ClientProxy client) {
		boolean removed;

		synchronized (clients) {
			removed = clients.remove(client);
		}

		if (removed) {
			try {
				client.close();
			} catch (IOException e) {
				System.err.println("Fehler beim Schließen eines Clients: " + e);
			}
		}

		return removed;
	}

	/// Fügt ein Paket zur Broadcast-Queue hinzu, damit es an alle verbundenen Clients gesendet wird.
	/// Gibt `true` zurück, wenn das Paket erfolgreich zur Queue hinzugefügt wurde.
	public boolean broadcast(Packet packet) throws InterruptedException {
		if (stopFlag.get()) {
			return true;
		}

		return broadcastPacketQueue.offer(packet);
	}

	public void shutdown() {
		stopFlag.set(true);
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
