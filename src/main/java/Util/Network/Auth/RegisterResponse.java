package Util.Network.Auth;

import User.Login.Status;

public class RegisterResponse extends Auth {
	private final Status status;
	private final String message;

	public RegisterResponse(Status status, String message) {
		this.status = status;
		this.message = message;
	}

	public Status getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
