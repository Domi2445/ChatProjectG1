package User.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "removed_group_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "username"}))
public class RemovedGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(nullable = false)
    private String username;

    protected RemovedGroupMember() {}

    public RemovedGroupMember(String groupId, String username) {
        this.groupId = groupId;
        this.username = username;
    }
}
