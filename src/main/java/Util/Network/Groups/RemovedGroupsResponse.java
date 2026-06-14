package Util.Network.Groups;

import Util.Network.Packet;
import java.util.Map;

public class RemovedGroupsResponse extends Packet {
    // groupId -> groupName
    private final Map<String, String> removedGroups;

    public RemovedGroupsResponse(Map<String, String> removedGroups) {
        this.removedGroups = removedGroups;
    }

    public Map<String, String> getRemovedGroups() { return removedGroups; }
}
