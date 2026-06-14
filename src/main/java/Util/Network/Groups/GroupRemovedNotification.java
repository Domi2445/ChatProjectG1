package Util.Network.Groups;

import Util.Network.Packet;
import java.util.UUID;

public class GroupRemovedNotification extends Packet {
    private final UUID groupId;
    private final String groupName;
    private final String removedBy;

    public GroupRemovedNotification(UUID groupId, String groupName, String removedBy) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.removedBy = removedBy;
    }

    public UUID getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getRemovedBy() { return removedBy; }
}
