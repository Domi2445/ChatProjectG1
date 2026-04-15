package Util;

import java.io.*;
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
