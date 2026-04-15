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
		this.in  = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void close() throws IOException {
		in.close();
		out.close();
		socket.close();
	}

	public void setStopFlag() {
		stopFlag.set(true);
	}

	public boolean getStopFlag() {
		return stopFlag.get();
	}
}
