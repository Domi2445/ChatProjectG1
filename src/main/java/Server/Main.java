package Server;

import java.io.IOException;

public class Main {
	private static final int PORT = 6969;

	public static void main(String[] args) {

		//UDP relay starten
		new Thread(new AudioRelayServer(8000),"AudioRelayServer").start();


		Server server;

		try {
			server = new Server(PORT);
		} catch (IOException e) {
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
