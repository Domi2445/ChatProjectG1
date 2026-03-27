package Server;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		try {
			new Server(6969).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
