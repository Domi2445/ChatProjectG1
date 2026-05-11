package Server;

import User.Repository.JPAUserRepository;
import User.Repository.UserRepository;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
	protected final ExecutorService threadExecutor = Executors.newVirtualThreadPerTaskExecutor();

	private final ServerSocket server;
	private final PacketBroker packetBroker;
	private final Future<?> packetBrokerFuture;

	public Server(int port) throws Exception {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		try (InputStream keystoreInput = Server.class.getResourceAsStream("/server.keystore")) {
			if (keystoreInput == null) {
				throw new IOException("Server-Keystore (server.keystore) nicht gefunden!");
			}
			keyStore.load(keystoreInput, "password".toCharArray());
		}
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, "password".toCharArray());
		sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

		SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
		server = ssf.createServerSocket(port);

		UserRepository userRepository = new JPAUserRepository();
		AuthHandler authHandler = new AuthHandler(userRepository);

		GeminiHandler geminiHandler = new GeminiHandler();
		packetBroker = new PacketBroker(threadExecutor, authHandler, geminiHandler);
		packetBrokerFuture = threadExecutor.submit(packetBroker);
	}

	@Override
	public void run() {

		// Audio
		new Thread(new AudioRelayServer(3298), "AudioRelayServer").start();
		// Video
		new Thread(new VideoRelayServer(9001), "VideoRelayServer").start();

		while (!Thread.currentThread().isInterrupted()) {
			try {
				Socket s = server.accept();

				SocketProxy socket = new SocketProxy(s);
				if (packetBroker.register(socket)) {
					System.out.println("Neuen Client registriert");
				} else {
					System.err.println("Maximale Anzahl an Clients erreicht, Verbindung abgelehnt");
					try { socket.close(); } catch (IOException ignored) {}
				}

			} catch (IOException e) {
				if (server.isClosed()) {
					break;
				}
				System.err.println("Fehler beim Akzeptieren eines neuen Clients: " + e);
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
