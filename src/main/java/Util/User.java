package Util;

import java.io.Serializable;

public class User implements Sender, Serializable {
    private final String username;

    public User(String username) {
        this.username = username;
    }

    @Override
    public String getIdentifier() {
        return username;
    }

	@Override
	public String toString() {
		return getIdentifier();
	}
}
