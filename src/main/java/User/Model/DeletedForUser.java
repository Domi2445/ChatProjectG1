package User.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "deleted_for_user", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "username"}))
public class DeletedForUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", nullable = false)
	private Long messageId;

	@Column(nullable = false)
	private String username;

	public DeletedForUser() {}

	public DeletedForUser(Long messageId, String username) {
		this.messageId = messageId;
		this.username = username;
	}

	public Long getMessageId() { return messageId; }
	public String getUsername() { return username; }
}
