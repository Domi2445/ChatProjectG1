package Util;

public class User implements Sender {
    private final String username;

    public User(String username) {
        this.username = username;
    }

    @Override
    public String getIdentifier() {
        return username;
    }
}