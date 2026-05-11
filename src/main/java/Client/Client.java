package Client;

import Util.Network.Packet;
import Util.Network.SocketProxy;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
		this.socket = new SocketProxy(socket);
		socket.setSoTimeout(100);
		this.out = out;
		this.in = in;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Packet packet = (Packet) socket.getInputStream().readObject();
				in.put(packet);
			} catch (SocketTimeoutException ignored) {
			} catch (IOException e) {
				// todo: Anzeige in der GUI, dass die Verbindung zum Server getrennt wurde
				System.err.println("Verbindung zum Server getrennt:\n" + e);
				break;
			} catch (ClassNotFoundException | InterruptedException e) {
				throw new RuntimeException(e);
			}

			Packet packet = out.poll();
			if (packet != null) {
				try {
					socket.getOutputStream().reset();
					socket.getOutputStream().writeObject(packet);
					socket.getOutputStream().flush();
				} catch (IOException e) {
					System.err.println("Fehler beim Senden:\n" + e);
					break;
				}
			}
		}
	}
}
