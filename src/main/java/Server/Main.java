package Server;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		// TCP Chat-Server
		try
		{
			Thread tcpThread = new Thread(new Server(6969), "TCPServer");
			tcpThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// UDP Audio Relay Server
		Thread udpThread = new Thread(new AudioRelayServer(9000), "AudioRelayServer");
		udpThread.setDaemon(true);
		udpThread.start();
	}
}