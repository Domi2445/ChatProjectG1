package Util.Emoji;

public class EmojiTest {

    public static void main(String[] args) {

        EmojiService service = new EmojiService();
        var emojis = service.loadEmojis();

        emojis.forEach(e -> System.out.println(e.getCharacter()));

        var emojisGeladen = service.loadEmojis();
    }
}
