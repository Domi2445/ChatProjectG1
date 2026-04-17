package Util.Network.Notifications;

import Util.User;

import java.io.Serial;

public class JoinNotification extends Notification {
	@Serial
	private static final long serialVersionUID = 1L;

	private final User user;

	public JoinNotification(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}
}
