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

    @Override
    public String getContent() {
        return emoji;
    }

    @Override
    public String getDisplayText() {
        return emoji;
    }

    @Override
    public void send() {
        System.out.println(getSender().getUsername() + " sendet Emoji: " + emoji);
    }
}