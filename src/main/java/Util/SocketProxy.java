package Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketProxy implements AutoCloseable {
	public final Socket socket;
	public final ObjectInputStream in;
	public final ObjectOutputStream out;
	private final AtomicBoolean stopFlag = new AtomicBoolean(false);

	public SocketProxy(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new ObjectOutputStream(socket.getOutputStream());
		this.out.flush();
		this.in = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void close() throws IOException {
		IOException exception = null;

		try {
			if (socket != null) socket.close();
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

	public void setStopFlag() {
		stopFlag.set(true);
	}

	public boolean getStopFlag() {
		return stopFlag.get();
	}
}
