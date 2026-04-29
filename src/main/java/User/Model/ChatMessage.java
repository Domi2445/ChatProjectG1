package User.Model;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_messages")
public class ChatMessage implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // Oder UUID, je nach DB
	private Long id;

	@Column(nullable = false)
	private String sender; // Username des Senders

	@Column
	private String receiver; // Username des Empfängers (für private Chats) oder null für Gruppen

	@Column
	private String chatRoomId; // Für Gruppenchats, optional

	@Column(nullable = false, length = 1000) // Für Text/Emoji; für Dateien: Dateiname oder UUID
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MessageType messageType;

	@Column
	private String filePath; // Optional: Pfad zur Datei im Dateisystem (für FILE)

	@Column(nullable = false)
	private LocalDateTime timestamp;

	public ChatMessage() {}

	public ChatMessage(String sender, String receiver, String chatRoomId, String content, MessageType messageType, String filePath) {
		this.sender = sender;
		this.receiver = receiver;
		this.chatRoomId = chatRoomId;
		this.content = content;
		this.messageType = messageType;
		this.filePath = filePath;
		this.timestamp = LocalDateTime.now();
	}

	// Getter und Setter
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getSender() { return sender; }
	public void setSender(String sender) { this.sender = sender; }

	public String getReceiver() { return receiver; }
	public void setReceiver(String receiver) { this.receiver = receiver; }

	public String getChatRoomId() { return chatRoomId; }
	public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public MessageType getMessageType() { return messageType; }
	public void setMessageType(MessageType messageType) { this.messageType = messageType; }

	public String getFilePath() { return filePath; }
	public void setFilePath(String filePath) { this.filePath = filePath; }

	public LocalDateTime getTimestamp() { return timestamp; }
	public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ChatMessage that = (ChatMessage) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "ChatMessage{" +
			"id=" + id +
			", sender='" + sender + '\'' +
			", receiver='" + receiver + '\'' +
			", chatRoomId='" + chatRoomId + '\'' +
			", content='" + content + '\'' +
			", messageType=" + messageType +
			", filePath='" + filePath + '\'' +
			", timestamp=" + timestamp +
			'}';
	}
}
