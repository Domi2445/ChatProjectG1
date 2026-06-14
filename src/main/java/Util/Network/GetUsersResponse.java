package Util.Network;

import User.Model.User;

import java.util.List;

public class GetUsersResponse extends Packet {
    private final List<User> users;

    public GetUsersResponse(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }
}
