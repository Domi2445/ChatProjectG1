package Util.Network.Groups;

import Util.Network.Packet;
import java.util.UUID;

public class GetGroupMembersRequest extends Packet {
    private final UUID groupId;

    public GetGroupMembersRequest(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getGroupId() { return groupId; }
}
