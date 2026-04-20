package Server;

import Util.Network.Groups.Group;
import Util.SocketProxy;
import Util.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager
{
	private final Map<UUID, Group> groups = new ConcurrentHashMap<>();
	private final Map<UUID, Set<SocketProxy>> groupMembers = new ConcurrentHashMap<>();
	private final Map<SocketProxy, User> clientUsers = new ConcurrentHashMap<>();

	// ---- client registration ----

	public void registerClient(SocketProxy client, User user)
	{
		clientUsers.put(client, user);
	}

	public void unregisterClient(SocketProxy client)
	{
		clientUsers.remove(client);
		for (Set<SocketProxy> members : groupMembers.values())
			members.remove(client);
	}

	public User getUser(SocketProxy client)
	{
		return clientUsers.get(client);
	}

	// ---- group management ----

	public Group createGroup(String name, SocketProxy creator)
	{
		User user = clientUsers.get(creator);
		String creatorName = user != null ? user.getUsername() : "Unknown";

		UUID id = UUID.randomUUID();
		Group group = new Group(id, name, creatorName);

		groups.put(id, group);
		Set<SocketProxy> members = Collections.synchronizedSet(new HashSet<>());
		members.add(creator);
		groupMembers.put(id, members);

		return group;
	}

	public boolean joinGroup(UUID groupId, SocketProxy client)
	{
		Set<SocketProxy> members = groupMembers.get(groupId);
		if (members == null) return false;
		members.add(client);
		return true;
	}

	public boolean leaveGroup(UUID groupId, SocketProxy client)
	{
		Set<SocketProxy> members = groupMembers.get(groupId);
		if (members == null) return false;
		return members.remove(client);
	}

	public boolean isMember(UUID groupId, SocketProxy client)
	{
		Set<SocketProxy> members = groupMembers.get(groupId);
		return members != null && members.contains(client);
	}

	public Set<SocketProxy> getGroupMembers(UUID groupId)
	{
		return groupMembers.getOrDefault(groupId, Collections.emptySet());
	}

	public Group getGroup(UUID groupId)
	{
		return groups.get(groupId);
	}

	public Collection<Group> getAllGroups()
	{
		return groups.values();
	}
}
