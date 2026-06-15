package User.Model;

import jakarta.persistence.*;

// chatRef: groupId (UUID string) für Gruppen, "private:<username>" für private Chats
@Entity
@Table(name = "hidden_chats", uniqueConstraints = @UniqueConstraint(columnNames = {"username", "chat_ref"}))
public class HiddenChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "chat_ref", nullable = false)
    private String chatRef;

    protected HiddenChat() {}

    public HiddenChat(String username, String chatRef) {
        this.username = username;
        this.chatRef = chatRef;
    }

    public String getChatRef() { return chatRef; }
}
