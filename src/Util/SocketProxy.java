package Util;

import java.io.*;
import java.net.Socket;

public class SocketProxy {
	public final Socket socket;
	public final BufferedReader in;
	public final BufferedWriter out;

	public SocketProxy(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}
}
