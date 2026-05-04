package Util.Network.Auth;

public class UserSearchRequest extends Auth {
	private final String query;

	public UserSearchRequest(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}
}
