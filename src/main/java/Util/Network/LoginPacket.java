package Util.Network;

import Util.User;
import java.io.Serial;

public class LoginPacket extends Packet
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final User user;

	public LoginPacket(User user)
	{
		this.user = user;
	}

	public User getUser() { return user; }
}
