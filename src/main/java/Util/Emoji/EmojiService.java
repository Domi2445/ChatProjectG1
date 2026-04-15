package Util.Emoji;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;

public class EmojiService {

    private static final String API_KEY = "3GL42ShUhjxk0Uxp0dzOkGnBp1KoEM9g5UIAEQEA";

    public static List<String> loadEmojis() {
        List<String> emojis = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.api-ninjas.com/v1/emoji?name=face"))
                    .header("X-Api-Key", API_KEY)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            // 🔥 QUICK & DIRTY Parsing (für Demo)
            String[] parts = body.split("\"character\":\"");

            for (int i = 1; i < parts.length; i++) {
                String emoji = parts[i].substring(0, 2); // Emoji rausziehen
                emojis.add(emoji);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return emojis;
    }
}