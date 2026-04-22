package Util.Network.Auth;

public class RegisterRequest extends Auth {
	private final String username;
	private final String displayname;
	private final String password;

	public RegisterRequest(String username, String displayname, String password) {
		this.username = username;
		this.displayname = displayname;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getDisplayname() {
		return displayname;
	}

	public String getPassword() {
		return password;
	}
}

