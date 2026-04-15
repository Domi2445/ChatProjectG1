package Util.Emoji;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class EmojiService {

    private static final String API_KEY = "DEIN_API_KEY_HIER";

    public static List<Emoji> loadEmojis() {
        List<Emoji> emojis = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.api-ninjas.com/v1/emoji?name=face"))
                    .header("X-Api-Key", API_KEY)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            String[] parts = body.split("\"character\":\"");

            for (int i = 1; i < parts.length; i++) {
                String character = parts[i].substring(0, 2);

                String namePart = parts[i].split("\"name\":\"")[1];
                String name = namePart.substring(0, namePart.indexOf("\""));

                Emoji emoji = new Emoji(i, name, character);
                emojis.add(emoji);

               
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return emojis;
    }
}