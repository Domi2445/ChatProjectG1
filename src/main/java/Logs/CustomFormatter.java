package Logs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		String timestamp = LocalDateTime.now()
			.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

		return "[" + timestamp + "] " + record.getMessage() + "\n";
	}
}
