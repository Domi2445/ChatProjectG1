package Util.Network.Messages;

import User.Model.User;

import java.io.Serial;

public class EmojiMessage extends Message {
	@Serial
	private static final long serialVersionUID = 1L;

    private final String emoji;

    public EmojiMessage(User sender, String emoji) {
        super(sender);
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }
}
