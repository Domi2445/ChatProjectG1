package Client;

import java.io.IOException;

public class Main {
	static void main() {
		try {
			new Controller().run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
