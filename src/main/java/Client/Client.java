package Client;

import Util.Network.ConnectionClosed;
import Util.Network.Packet;
import Util.Network.SocketProxy;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.concurrent.BlockingQueue;

public class Client implements Runnable {
	private final BlockingQueue<Packet> out;
	private final BlockingQueue<Packet> in;
	private final SocketProxy socket;

	public Client(String ip, int port, BlockingQueue<Packet> out, BlockingQueue<Packet> in) throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
				public void checkClientTrusted(X509Certificate[] certs, String authType) {}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
			}
		};

		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		SSLSocketFactory ssf = sc.getSocketFactory();

		Socket socket = ssf.createSocket(ip, port);
		// No setSoTimeout: a timeout on the read socket causes SocketTimeoutException
		// mid-deserialization of large packets, corrupting the ObjectInputStream state.
		this.socket = new SocketProxy(socket);
		this.out = out;
		this.in = in;
	}

	@Override
	public void run() {
		Thread sender = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					Packet packet = out.take();
					try {
						socket.getOutputStream().reset();
						socket.getOutputStream().writeObject(packet);
						socket.getOutputStream().flush();
					} catch (IOException e) {
						System.err.println("Fehler beim Senden:\n" + e);
						in.offer(new ConnectionClosed("Verbindung zum Server unterbrochen"));
						return;
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "ClientSendThread");
		sender.setDaemon(true);
		sender.start();

		try {
			while (true) {
				try {
					Packet packet = (Packet) socket.getInputStream().readObject();
					in.put(packet);
				} catch (IOException e) {
					System.err.println("Verbindung zum Server getrennt:\n" + e);
					in.offer(new ConnectionClosed("Verbindung zum Server getrennt"));
					break;
				} catch (ClassNotFoundException e) {
					System.err.println("Ungültiges Paket empfangen:\n" + e);
					in.offer(new ConnectionClosed("Ungültiges Paket vom Server empfangen"));
					break;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		} finally {
			sender.interrupt();
		}
	}
}
