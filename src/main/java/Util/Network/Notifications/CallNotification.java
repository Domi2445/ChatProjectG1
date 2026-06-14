package Util.Network.Notifications;
import User.Model.User;

import java.io.Serial;
import java.io.Serializable;
public class CallNotification extends Notification
{
	@Serial
	private static final long serialVersionUID = 1L;

	public enum  CallType{ REQUEST,ACCEPT,REJECT,END}

	private final CallType type;
	private final User sender ;
	private final String targetUsername;// empfngr
	private final int audioPort;//sender audio-empfngrport
	private final boolean video;// true = Videoanruf (Audio + Video), false = reiner Audioanruf
	private String senderIp;// vom server gesetzt

	public CallNotification(CallType type, User sender, String targetUsername, int audioPort)
	{
		this(type, sender, targetUsername, audioPort, false);
	}

	public CallNotification(CallType type, User sender, String targetUsername, int audioPort, boolean video)
	{
		this.type           = type;
		this.sender         = sender;
		this.targetUsername = targetUsername;
		this.audioPort      = audioPort;
		this.video          = video;
	}

		public CallType getType()             { return type; }
		public User getSender()               { return sender; }
		public String getTargetUsername()     { return targetUsername; }
		public int getAudioPort()             { return audioPort; }
		public boolean isVideo()              { return video; }
		public String getSenderIp()           { return senderIp; }
		public void setSenderIp(String ip)    { this.senderIp = ip; }

	}


