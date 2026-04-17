package Logs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class LogHandler {
	private static Path path = Path.of("src/main/java/Logs/log.txt");
	public static void createLogEntry(String enterCurrentClassHere, String logMessage) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toString(), true))) {
			writer.write(new LogEntry(enterCurrentClassHere, logMessage).toString());
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
