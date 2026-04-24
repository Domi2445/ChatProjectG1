package Logs;

import java.security.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {
	private String prefix; //File-Name
	private String message;
	private String timestamp;
	//			(timestamp)				(prefix)		(message)
	//Example: [2026/04/17 11:38:33][LoginValidator] IllegalArgumentException: Nutzername darf nicht leer sein.
	public LogEntry(String prefix, String message) {
		this.prefix = prefix;
		this.message = message;
		this.timestamp = nowFormatted();
	}

	public static String nowFormatted() {
		return LocalDateTime.now()
			.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "[" + timestamp + "][" + prefix + "] " + message;
	}
}
