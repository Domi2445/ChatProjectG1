package Server;

import java.io.IOException;

public class Main {
	private static final int DEFAULT_PORT = 3299;

	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("server.port",
			System.getenv().getOrDefault("SERVER_PORT", String.valueOf(DEFAULT_PORT))));

		Server server;

		try {
			server = new Server(port);
		} catch (Exception e) {
			System.err.println("Fehler beim Starten des Servers: " + e);
			return;
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				server.stop();
			} catch (IOException e) {
				System.err.println("Fehler beim Stoppen des Servers: " + e);
			}
		}));

		server.run();
	}
}
