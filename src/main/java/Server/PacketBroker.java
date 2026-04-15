package Server;

import Util.Network.Packet;
import Util.SocketProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/// Verteilt Pakete an alle angemeldeten Clients
public class PacketBroker implements Runnable {
	private final BlockingQueue<Packet> packetBrokerQueue;
	private final List<SocketProxy> clients;

	public PacketBroker(BlockingQueue<Packet> packetBrokerQueue, List<SocketProxy> clients) {
		this.packetBrokerQueue = packetBrokerQueue;
		this.clients = clients;
	}

	@Override
	public void run() {
		while (true) {
			ArrayList<SocketProxy> clientsToRemove = new ArrayList<>();

			try {
				Packet packet = packetBrokerQueue.take();

				synchronized (clients) {
					for (SocketProxy client : clients) {
						if (client.socket.isClosed()) {
							clientsToRemove.add(client);
							continue;
						} else if (client.getStopFlag()) {
							clientsToRemove.add(client);
							try { client.close(); } catch (IOException ignored) {}
							continue;
						}

						try {
							client.out.writeObject(packet);
							client.out.flush();
						} catch (IOException e) {
							System.out.println("Fehler beim Senden:\n" + e);
							try { client.close(); } catch (IOException ignored) {}
							clientsToRemove.add(client);
						}
					}

					clients.removeAll(clientsToRemove);
				}

			} catch (InterruptedException e) {
				break;
			}

			clientsToRemove.clear();
		}
	}
}
