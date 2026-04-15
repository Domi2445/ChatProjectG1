package Util.Emoji;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class EmojiService {

    private static final String API_KEY = "3GL42ShUhjxk0Uxp0dzOkGnBp1KoEM9g5UIAEQEA";
    private static final String API_URL = "https://api.api-ninjas.com/v1/emoji?name=face";

    public List<Emoji> loadEmojis() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("X-Api-Key", API_KEY)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return parseResponse(response.body());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Emoji> parseResponse(String body) {
        List<Emoji> emojis = new ArrayList<>();
        String[] entries = body.split("\\{");

        for (int i = 1; i < entries.length; i++) {
            try {
                // "code": extrahieren
                String codePart = entries[i].split("\"code\": \"")[1];
                String code = codePart.substring(0, codePart.indexOf("\""));

                // "name": extrahieren
                String namePart = entries[i].split("\"name\": \"")[1];
                String name = namePart.substring(0, namePart.indexOf("\""));

                // U+1F600 in Emoji umwandeln
                int codePoint = Integer.parseInt(code.replace("U+", ""), 16);
                String character = new String(Character.toChars(codePoint));

                emojis.add(new Emoji(i, name, character));

            } catch (Exception e) {
                System.err.println("Fehler beim Parsen eines Emojis: " + e.getMessage());
            }
        }

        return emojis;
    }
}