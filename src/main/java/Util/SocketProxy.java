package Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketProxy {
	public final Socket socket;
	public final ObjectInputStream in;
	public final ObjectOutputStream out;

	public SocketProxy(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new ObjectOutputStream(socket.getOutputStream());
		this.in  = new ObjectInputStream(socket.getInputStream());
	}
}
