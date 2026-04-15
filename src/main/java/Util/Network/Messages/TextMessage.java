package Util.Network.Messages;

import Util.User;

import java.io.Serial;
import java.io.Serializable;

public class TextMessage extends Message implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String content;

    public TextMessage(User sender, String content) {
        super(sender);
        this.content = content;
    }

    public String getContent() { return content; }
}
