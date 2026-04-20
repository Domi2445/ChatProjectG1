package Util.Network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/// Kapselt einen Socket und die zugehörigen Ein- und Ausgabestreams.
public class SocketProxy implements AutoCloseable {
	private final Socket socket;
	public final ObjectInputStream in;
	public final ObjectOutputStream out;

	public SocketProxy(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new ObjectOutputStream(socket.getOutputStream());
		this.out.flush();
		this.in = new ObjectInputStream(socket.getInputStream());
	}

	public boolean isClosed() {
		return socket.isClosed();
	}

	@Override
	public void close() throws IOException {
		IOException exception = null;

		try {
			if (!socket.isClosed()) socket.close();
		} catch (IOException e) {
			exception = e;
		}

		try {
			if (in != null) in.close();
		} catch (IOException e) {
			if (exception == null) exception = e;
		}

		try {
			if (out != null) out.close();
		} catch (IOException e) {
			if (exception == null) exception = e;
		}

		if (exception != null) throw exception;
	}
}
