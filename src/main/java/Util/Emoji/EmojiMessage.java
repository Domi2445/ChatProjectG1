package Util.Emoji;

import Util.Network.Messages.Message;
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
}
