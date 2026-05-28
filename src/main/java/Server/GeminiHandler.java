package Server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import Util.EnvLoader;
public class GeminiHandler {
	private final HttpClient httpClient;
	private final String apiKey;
	private final String MODEL = "gemini-3-flash-preview";

	public GeminiHandler() throws IOException {
		this.httpClient = HttpClient.newBuilder().build();
		this.apiKey = EnvLoader.loadGeminiKey();
	}

	public HttpRequest buildRequest(String prompt) {
		String jsonBody = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": "%s" }
                  ]
                }
              ]
            }
            """.formatted(escapeJson(prompt));

		return HttpRequest.newBuilder()
			.uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + apiKey))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
			.timeout(Duration.ofSeconds(10))
			.build();
	}

	public String ask(String prompt) throws IOException, InterruptedException {

		String responseBody = sendPrompt(prompt);
		return extractText(responseBody);
	}

	public String sendPrompt(String prompt) throws IOException, InterruptedException {
		HttpRequest request = buildRequest(prompt);
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IllegalStateException("Keine Verbindung zur API: " + response.statusCode());
		}

		return response.body();
	}

	private String extractText(String json) {
		String marker = "\"text\":";
		int markerIndex = json.indexOf(marker);
		if (markerIndex == -1) {
			throw new IllegalStateException("Kein Antwort-Text in Gemini-Response gefunden: " + json);
		}

		int firstQuote = json.indexOf("\"", markerIndex + marker.length());
		int secondQuote = json.indexOf("\"", firstQuote + 1);

		if (firstQuote == -1 || secondQuote == -1) {
			throw new IllegalStateException("Antwort-Text konnte nicht geparst werden: " + json);
		}

		return json.substring(firstQuote + 1, secondQuote)
			.replace("\\n", "\n")
			.replace("\\r", "\r")
			.replace("\\\"", "\"")
			.replace("\\\\", "\\");
	}

	private static String escapeJson(String text) {
		return text
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r");
	}
}
