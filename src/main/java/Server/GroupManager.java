package Server;

import Util.Network.Groups.Group;
import User.Model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager
{
	private final Map<UUID, Group> groups = new ConcurrentHashMap<>();
	private final Map<UUID, Set<ClientProxy>> groupMembers = new ConcurrentHashMap<>();
	private final Map<ClientProxy, User> clientUsers = new ConcurrentHashMap<>();

	// ---- client registration ----

	public void registerClient(ClientProxy client, User user)
	{
		clientUsers.put(client, user);
	}

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

	public boolean joinGroup(UUID groupId, ClientProxy client)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		if (members == null) return false;
		members.add(client);
		return true;
	}

	public boolean leaveGroup(UUID groupId, ClientProxy client)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		if (members == null) return false;
		return members.remove(client);
	}

	public boolean isMember(UUID groupId, ClientProxy client)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		return members != null && members.contains(client);
	}

	public Set<ClientProxy> getGroupMembers(UUID groupId)
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
