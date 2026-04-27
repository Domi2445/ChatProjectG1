package Server;

import Util.Network.SocketProxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
	protected final ExecutorService threadExecutor = Executors.newVirtualThreadPerTaskExecutor();

	private final ServerSocket server;
	private final PacketBroker packetBroker;
	private final Future<?> packetBrokerFuture;

	public Server(int port) throws IOException {
		server = new ServerSocket(port);
		packetBroker = new PacketBroker(threadExecutor);
		packetBrokerFuture = threadExecutor.submit(packetBroker);
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

			} catch (IOException e) {
				if (!server.isClosed()) {
					System.err.println("Fehler beim Akzeptieren eines neuen Clients: " + e);
				}
				break;
			}
		}
	}

	public void stop() throws IOException {
		System.out.println("Server wird heruntergefahren...");

		packetBroker.shutdown();
		packetBrokerFuture.cancel(true);
		server.close();
		threadExecutor.shutdownNow();

		try {
			if (!threadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				System.err.println("Server-Threads konnten nicht rechtzeitig beendet werden");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
