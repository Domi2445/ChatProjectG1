package Util.Network.Notifications;

import User.Model.User;

import java.io.Serial;

public class LeaveNotification extends Notification {
	@Serial
	private static final long serialVersionUID = 1L;

	private final User user;

	public LeaveNotification(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}
}
