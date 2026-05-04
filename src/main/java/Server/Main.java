package Server;

import java.io.IOException;

public class Main {
	private static final int PORT = 433;

	public static void main(String[] args) {
		Server server;

		try {
			server = new Server(PORT);
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
