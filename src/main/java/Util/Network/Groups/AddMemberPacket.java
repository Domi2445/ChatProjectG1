package Util.Network.Groups;

import Util.Network.Packet;

import java.io.Serial;
import java.util.UUID;

public class AddMemberPacket extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID groupId;
    private final String username;

    public AddMemberPacket(UUID groupId, String username) {
        this.groupId = groupId;
        this.username = username;
    }

    public UUID getGroupId() { return groupId; }
    public String getUsername() { return username; }
}
