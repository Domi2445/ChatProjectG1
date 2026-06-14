package Server;

import User.Model.ChatGroup;
import User.Model.User;
import User.Repository.GroupRepository;
import Util.Network.Groups.Group;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager
{
	public static final UUID BROADCAST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	public static final String BROADCAST_NAME = "📢 Ankündigungen";

	private final Map<UUID, Group> groups = new ConcurrentHashMap<>();
	private final Map<UUID, Set<ClientProxy>> groupMembers = new ConcurrentHashMap<>();
	private final Map<ClientProxy, User> clientUsers = new ConcurrentHashMap<>();
	private final GroupRepository groupRepository;

	public GroupManager(GroupRepository groupRepository) {
		this.groupRepository = groupRepository;

		groupRepository.saveGroup(BROADCAST_ID, BROADCAST_NAME, "System");
		Group broadcast = new Group(BROADCAST_ID, BROADCAST_NAME, "System");
		groups.put(BROADCAST_ID, broadcast);
		groupMembers.put(BROADCAST_ID, Collections.synchronizedSet(new HashSet<>()));

		for (ChatGroup cg : groupRepository.getAllGroups()) {
			UUID id = cg.getId();
			if (groups.containsKey(id)) continue;
			Group g = new Group(id, cg.getName(), cg.getCreatorUsername());
			groups.put(id, g);
			groupMembers.put(id, Collections.synchronizedSet(new HashSet<>()));
		}
	}

	public void registerClient(ClientProxy client, User user)
	{
		clientUsers.put(client, user);

		// Broadcast nur beitreten wenn der User nicht explizit entfernt wurde
		List<String> removedIds = groupRepository.getRemovedGroupIdsForUser(user.getUsername());
		if (!removedIds.contains(BROADCAST_ID.toString())) {
			groupMembers.get(BROADCAST_ID).add(client);
		}

		for (String groupIdStr : groupRepository.getGroupIdsForUser(user.getUsername())) {
			try {
				UUID groupId = UUID.fromString(groupIdStr);
				Set<ClientProxy> members = groupMembers.get(groupId);
				if (members != null) members.add(client);
			} catch (IllegalArgumentException ignored) {}
		}
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

	public Group createGroup(String name, ClientProxy creator)
	{
		User user = clientUsers.get(creator);
		String creatorName = user != null ? user.getUsername() : "Unknown";

		UUID id = UUID.randomUUID();
		Group group = new Group(id, name, creatorName);

		groupRepository.saveGroup(id, name, creatorName);
		if (user != null) groupRepository.addMember(id, creatorName);

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

		User user = clientUsers.get(client);
		if (user != null) groupRepository.addMember(groupId, user.getUsername());

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

	public Collection<Group> getGroupsForClient(ClientProxy client)
	{
		List<Group> result = new ArrayList<>();
		for (Map.Entry<UUID, Set<ClientProxy>> entry : groupMembers.entrySet())
		{
			if (entry.getValue().contains(client))
				result.add(groups.get(entry.getKey()));
		}
		return result;
	}

	public List<String> getRemovedGroupIdsForUser(String username)
	{
		return groupRepository.getRemovedGroupIdsForUser(username);
	}

	public Set<String> getMemberUsernames(UUID groupId)
	{
		List<String> fromDb = groupRepository.getMembersForGroup(groupId.toString());
		return new java.util.HashSet<>(fromDb);
	}

	public boolean addMemberByUsername(UUID groupId, String username, Map<String, ClientProxy> onlineClients)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		if (members == null) return false;

		groupRepository.addMember(groupId, username);

		ClientProxy client = onlineClients.get(username);
		if (client != null) members.add(client);

		return true;
	}

	// Gibt den ClientProxy des entfernten Users zurück (null wenn offline), damit der Server ihm eine Benachrichtigung schicken kann.
	public ClientProxy removeMemberByUsername(UUID groupId, String username, Map<String, ClientProxy> onlineClients)
	{
		Set<ClientProxy> members = groupMembers.get(groupId);
		if (members == null) return null;

		groupRepository.removeMember(groupId, username);
		groupRepository.markAsRemoved(groupId, username);

		ClientProxy client = onlineClients.get(username);
		if (client != null) members.remove(client);

		return client;
	}
}
