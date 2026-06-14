package User.Model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "chat_groups")
public class ChatGroup {
    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "creator_username", nullable = false)
    private String creatorUsername;

    protected ChatGroup() {}

    public ChatGroup(UUID id, String name, String creatorUsername) {
        this.id = id.toString();
        this.name = name;
        this.creatorUsername = creatorUsername;
    }

    public UUID getId() { return UUID.fromString(id); }
    public String getName() { return name; }
    public String getCreatorUsername() { return creatorUsername; }
}
