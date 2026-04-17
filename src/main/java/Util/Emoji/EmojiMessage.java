package Util.Emoji;

import Util.Message.Message;
import Util.User;

public class EmojiMessage extends Message {

    private final String emoji;

    public EmojiMessage(User sender, String emoji) {
        super(sender);
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getContent() {
        return emoji;
    }

    public String getDisplayText() {
        return emoji;
    }

    public void send() {
        System.out.println(getSender().getUsername() + " sendet Emoji: " + emoji);
    }
}
