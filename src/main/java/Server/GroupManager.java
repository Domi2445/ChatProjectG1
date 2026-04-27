package Server;

import Util.Network.Groups.Group;
import User.Model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// manages all groups on the server side.
// keeps track of which groups exist, which clients are in each group, and which user belongs to each client.
// this class is only used internally by the server — clients interact with groups by sending packets.
public class GroupManager
{
	private final Map<UUID, Group> groups = new ConcurrentHashMap<>();
	// maps each group id to the set of clients currently in that group
	private final Map<UUID, Set<ClientProxy>> groupMembers = new ConcurrentHashMap<>();
	// maps each connected client to their logged-in user object
	private final Map<ClientProxy, User> clientUsers = new ConcurrentHashMap<>();

	// ---- client registration ----

	// call this after a client successfully logs in so the group manager knows who they are
	public void registerClient(ClientProxy client, User user)
	{
		clientUsers.put(client, user);
	}

	// call this when a client disconnects — removes them from all groups automatically
	public void unregisterClient(ClientProxy client)
	{
		clientUsers.remove(client);
		for (Set<ClientProxy> members : groupMembers.values())
			members.remove(client);
	}

	public User getUser(ClientProxy client)
	{
		return clientUsers.get(client);
	}

	// ---- group management ----

	// creates a new group with a random uuid and adds the creator as the first member.
	// returns the created Group object which contains the id needed to join or send messages to the group.
	public Group createGroup(String name, ClientProxy creator)
	{
		User user = clientUsers.get(creator);
		String creatorName = user != null ? user.getUsername() : "Unknown";

		UUID id = UUID.randomUUID();
		Group group = new Group(id, name, creatorName);

		groups.put(id, group);
		Set<ClientProxy> members = Collections.synchronizedSet(new HashSet<>());
		members.add(creator);
		groupMembers.put(id, members);

		return group;
	}

	// adds a client to an existing group. returns false if the group doesn't exist.
	public boolean joinGroup(UUID groupId, ClientProxy client)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		if (members == null) return false;
		members.add(client);
		return true;
	}

	// removes a client from a group. returns false if the group doesn't exist.
	public boolean leaveGroup(UUID groupId, ClientProxy client)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		if (members == null) return false;
		return members.remove(client);
	}

	// checks if a client is currently in a group
	public boolean isMember(UUID groupId, ClientProxy client)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		return members != null && members.contains(client);
	}

	// returns all clients currently in a group — used by packetbroker to route group messages
	public Set<ClientProxy> getGroupMembers(UUID groupId)
	{
		return groupMembers.getOrDefault(groupId, Collections.emptySet());
	}

	public Group getGroup(UUID groupId)
	{
		return groups.get(groupId);
	}

	// returns all groups that currently exist on the server
	public Collection<Group> getAllGroups()
	{
		return groups.values();
	}
}
