package AudioCall;
import java.net.*;

public class UDPReciever {
	private DatagramSocket socket;
	private byte[] buffer;

	public UDPReciever(int port) throws Exception {
		this.socket = new DatagramSocket(port);
		this.buffer = new byte[1024];
	}

	public int getPort() {
		return socket.getLocalPort();
	}

	public byte[] receiver() throws Exception {
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
		return data;
	}

	public void close() {
		socket.close();
	}
}
