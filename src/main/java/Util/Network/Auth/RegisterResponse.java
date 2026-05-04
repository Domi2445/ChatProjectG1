package Util.Network.Auth;

import User.Login.Status;
import User.Model.User;

public class RegisterResponse extends Auth {
	private final Status status;
	private final String message;
	private final User user;

	public RegisterResponse(Status status, String message, User user) {
		this.status = status;
		this.message = message;
		this.user = user;
	}

	public Status getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public User getUser() {
		return user;
	}
}
