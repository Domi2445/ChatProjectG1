package Logs;

import java.io.IOException;
import java.util.logging.*;

public class LogHandler {

	private static final Logger logger = Logger.getLogger("Logger");

	static {
		try {
			FileHandler fileHandler = new FileHandler("Logs/log.txt", true);
			fileHandler.setFormatter(new CustomFormatter());
			logger.addHandler(fileHandler);

			logger.setUseParentHandlers(false);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void log(String prefix, String message) {
		logger.info("[" + prefix + "] " + message);
	}

	public static void error(String prefix, String message) {
		logger.severe("[" + prefix + "] " + message);	}
}
