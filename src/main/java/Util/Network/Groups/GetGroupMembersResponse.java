package Util.Network.Groups;

import Util.Network.Packet;
import java.util.Set;
import java.util.UUID;

public class GetGroupMembersResponse extends Packet {
    private final UUID groupId;
    private final Set<String> usernames;
    private final String creatorUsername;

    public GetGroupMembersResponse(UUID groupId, Set<String> usernames, String creatorUsername) {
        this.groupId = groupId;
        this.usernames = usernames;
        this.creatorUsername = creatorUsername;
    }

    public UUID getGroupId() { return groupId; }
    public Set<String> getUsernames() { return usernames; }
    public String getCreatorUsername() { return creatorUsername; }
}
