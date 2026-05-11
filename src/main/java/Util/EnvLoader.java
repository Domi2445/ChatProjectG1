package Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EnvLoader {
	public static String loadGeminiKey() throws IOException {
		String apiKey = System.getenv("GEMINI_API_KEY");
		if (apiKey != null && !apiKey.isBlank()) {
			return apiKey;
		}
		Path envPath = Path.of(".env");
		if (!Files.exists(envPath)) {
			throw new IllegalStateException("GEMINI_API_KEY nicht gefunden");
		}

		for (String line : Files.readAllLines(envPath)) {
			if (line.startsWith("GEMINI_API_KEY=")) {
				String value = line.substring("GEMINI_API_KEY=".length()).trim();
				if (!value.isBlank()) {
					return value;
				}
			}
		}

		throw new IllegalStateException("GEMINI_API_KEY nicht gefunden");
	}
}
