package Util.Network.Notifications;
import User.Model.User;

import java.io.Serial;
import java.io.Serializable;
public class CallNotification extends Notification
{
	@Serial
	private static final long serialVersionUID = 1L;

	public enum  CallType{ REQUEST,ACCEPT,REJECT}

	private final CallType type;
	private final User sender ;
	private final String targetUsername;// empfngr
	private final int audioPort;//sender audio-empfngrport
	private String senderIp;// vom server gesetzt

	public CallNotification(CallType type, User sender, String targetUsername, int audioPort)
	{
		this.type           = type;
		this.sender         = sender;
		this.targetUsername = targetUsername;
		this.audioPort      = audioPort;
	}

		public CallType getType()             { return type; }
		public User getSender()               { return sender; }
		public String getTargetUsername()     { return targetUsername; }
		public int getAudioPort()             { return audioPort; }
		public String getSenderIp()           { return senderIp; }
		public void setSenderIp(String ip)    { this.senderIp = ip; }

	}


