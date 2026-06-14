package Util.Network.Groups;

import Util.Network.Packet;
import java.util.UUID;

public class RemoveMemberPacket extends Packet {
    private final UUID groupId;
    private final String username;

    public RemoveMemberPacket(UUID groupId, String username) {
        this.groupId = groupId;
        this.username = username;
    }

    public UUID getGroupId() { return groupId; }
    public String getUsername() { return username; }
}
