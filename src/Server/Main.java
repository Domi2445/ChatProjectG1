package Server;

import java.io.IOException;

public class Main {
	static void main() {
		try {
			new Server(6969).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
