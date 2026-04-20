package Server;

import Util.Network.Notifications.JoinNotification;
import Util.Network.Packet;
import Util.SocketProxy;
import User.Model.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
	protected static final ExecutorService THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

	private final ServerSocket server;
	private final PacketBroker packetBroker;
	private final Future<?> packetBrokerFuture;

	public Server(int port) throws IOException {
		server = new ServerSocket(port);
		packetBroker = new PacketBroker();
		packetBrokerFuture = THREAD_EXECUTOR.submit(packetBroker);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Socket s = server.accept();

				SocketProxy socket = new SocketProxy(s);
				if (packetBroker.register(socket)) {
					System.out.println("Neuen Client registriert");
				} else {
					System.err.println("Maximale Anzahl an Clients erreicht, Verbindung abgelehnt");
					try { socket.close(); } catch (IOException ignored) {}
					continue;
				}

				// todo: Wenn es ein Loginsystem gibt, hier das User-Objekt des neu beigetretenen Clients übergeben, sobald dieser sich angemeldet hat
				User user = new User();
				user.setUsername("Platzhalter");
				packetBroker.broadcast(new JoinNotification(user));

			} catch (IOException e) {
				if (!e.getMessage().equals("Socket closed")) {
					System.err.println("Fehler beim Akzeptieren eines neuen Clients: " + e);
				}
				break;

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void stop() throws IOException {
		System.out.println("Server wird heruntergefahren...");

		packetBroker.shutdown();
		packetBrokerFuture.cancel(true);
		server.close();
		THREAD_EXECUTOR.shutdownNow();

		try {
			if (!THREAD_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
				System.err.println("Server-Threads konnten nicht rechtzeitig beendet werden");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
