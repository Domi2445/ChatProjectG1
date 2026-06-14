package User.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "group_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "username"}))
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(nullable = false)
    private String username;

    protected GroupMember() {}

    public GroupMember(String groupId, String username) {
        this.groupId = groupId;
        this.username = username;
    }

    public String getGroupId() { return groupId; }
    public String getUsername() { return username; }
}
