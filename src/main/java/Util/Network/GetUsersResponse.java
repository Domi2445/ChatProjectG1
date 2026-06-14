package Util.Network;

import User.Model.User;

import java.util.List;

public class GetUsersResponse extends Packet {
    private static final long serialVersionUID = 1L;
    private final List<User> users;

    public GetUsersResponse(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }
}
