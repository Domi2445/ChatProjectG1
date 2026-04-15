package Util.Network.Notifications;

import Util.User;

import java.io.Serial;
import java.io.Serializable;

public class JoinNotification extends Notification implements Serializable {
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
