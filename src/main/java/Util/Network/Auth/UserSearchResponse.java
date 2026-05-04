package Util.Network.Auth;

import User.Model.User;

import java.util.List;

public class UserSearchResponse extends Auth {
	private final List<User> results;

	public UserSearchResponse(List<User> results) {
		this.results = results;
	}

	public List<User> getResults() {
		return results;
	}
}
