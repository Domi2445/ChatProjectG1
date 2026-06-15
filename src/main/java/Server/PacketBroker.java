package Server;

import User.Model.ChatMessage;
import User.Model.MessageType;
import User.Model.User;
import User.Repository.ChatHistoryService;
import User.Repository.ChatMessageRepository;
import User.Repository.DeletedForUserRepository;
import User.Repository.JPAUserRepository;
import Util.FileUtil;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.RegisterRequest;
import Util.Network.DeleteForMeMessage;
import Util.Network.DeleteMessage;
import Util.Network.EditMessage;
import User.Repository.GroupRepository;
import Util.Network.Groups.AddMemberPacket;
import Util.Network.Groups.GetGroupMembersRequest;
import Util.Network.Groups.GetGroupMembersResponse;
import Util.Network.Groups.GroupRemovedNotification;
import Util.Network.Groups.RemoveMemberPacket;
import Util.Network.Groups.RemovedGroupsResponse;
import Util.Network.Groups.CreateGroupPacket;
import Util.Network.Groups.Group;
import Util.Network.Groups.GroupCreatedPacket;
import Util.Network.Groups.GroupListRequestPacket;
import Util.Network.Groups.GroupListResponsePacket;
import Util.Network.Groups.JoinGroupPacket;
import Util.Network.Groups.LeaveGroupPacket;
import Util.Network.Groups.MyGroupsRequestPacket;
import Util.Network.HistoryRequest;
import Util.Network.Messages.FileMessage;
import Util.Network.Messages.Message;
import Util.Network.Notifications.JoinNotification;
import Util.Network.Messages.TextMessage;
import Util.Network.Notifications.LeaveNotification;
import Util.Network.Packet;
import Util.Network.GetUsersRequest;
import Util.Network.GetUsersResponse;
import Util.Network.ReadReceipt;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

/// Verteilt Pakete an alle angemeldeten Clients.
public class PacketBroker implements Runnable {
	public static final int MAX_INCOMING_PACKETS = 128;
	public static final int MAX_OUTGOING_PACKETS = 128;
	public static final int MAX_CLIENTS = 16;

	private final ExecutorService threadExecutor;
	private final AuthHandler authHandler;
	private final GeminiHandler geminiHandler;
	private final ChatHistoryHandler chatHistoryHandler;
	private final ChatHistoryService chatHistoryService;
	private final DeletedForUserRepository deletedForUserRepository;
	private final GroupManager groupManager;
	private final JPAUserRepository userRepository;

	/// Queue für Pakete, die an alle verbundenen Clients gesendet werden sollen.
	private final BlockingQueue<IncomingPacket> broadcastPacketQueue;
	/// Liste aller aktuell verbundenen Clients.
	private final List<ClientProxy> clients;
	private final AtomicBoolean stopFlag;

	public PacketBroker(ExecutorService threadExecutor, AuthHandler authHandler, GeminiHandler geminiHandler) {
		this(threadExecutor, authHandler, geminiHandler, new ChatHistoryHandler(new ChatMessageRepository(), new JPAUserRepository()));
	}

	public PacketBroker(ExecutorService threadExecutor, AuthHandler authHandler, GeminiHandler geminiHandler, ChatHistoryHandler chatHistoryHandler) {
		this.threadExecutor = threadExecutor;
		this.authHandler = authHandler;
		this.geminiHandler = geminiHandler;
		this.chatHistoryHandler = chatHistoryHandler;
		this.chatHistoryService = new ChatHistoryService();
		this.deletedForUserRepository = new DeletedForUserRepository();
		this.groupManager = new GroupManager(new GroupRepository());
		this.userRepository = new JPAUserRepository();

		this.broadcastPacketQueue = new ArrayBlockingQueue<>(MAX_INCOMING_PACKETS);
		this.clients = new ArrayList<>(MAX_CLIENTS);
		this.stopFlag = new AtomicBoolean(false);
	}

	@Override
	public void run() {
		while (!stopFlag.get() && !Thread.currentThread().isInterrupted()) {
			try {
				cleanupDisconnectedClients();
				IncomingPacket incoming = broadcastPacketQueue.poll(3, TimeUnit.SECONDS);
				if (incoming == null) continue;
				Packet packet = incoming.packet();
				ClientProxy sender = incoming.sender();

				switch (packet) {
					case LoginRequest req -> {
						if (authHandler.handleLogin(req, sender)) {
							User user = sender.getUser();
							// register with group manager so this client can create/join groups
							groupManager.registerClient(sender, user);

							// History direkt nach Login senden
							chatHistoryHandler.handleHistoryRequest(new HistoryRequest(user.getUsername(), null, "main"), sender);

							// Dem Client die Gruppen schicken, in denen er Mitglied ist (inkl. Broadcast)
							sender.tryEnqueuePacket(new GroupListResponsePacket(groupManager.getGroupsForClient(sender)));
							// Dem Client mitteilen aus welchen Gruppen er entfernt wurde (für Lesezugriff)
							List<String> removedIds = groupManager.getRemovedGroupIdsForUser(user.getUsername());
							if (!removedIds.isEmpty()) {
								Map<String, String> removedMap = new java.util.HashMap<>();
								for (String gid : removedIds) {
									try {
										Group g = groupManager.getGroup(UUID.fromString(gid));
										removedMap.put(gid, g != null ? g.getName() : gid);
									} catch (IllegalArgumentException ignored) {}
								}
								sender.tryEnqueuePacket(new RemovedGroupsResponse(removedMap));
							}

										// Sende dem neu angemeldeten Client die bereits verbundenen Benutzer,
										// damit dieser deren Profilbilder direkt laden und cachen kann.
										synchronized (clients) {
											for (var existingClient : clients) {
												if (existingClient == sender) continue;
												var existingUser = existingClient.getUser();
												if (existingUser != null) {
													// versuche, dem neuen Client eine JoinNotification für den existierenden Benutzer zu senden
													sender.tryEnqueuePacket(new JoinNotification(existingUser));
												}
											}
										}

							try {
								if (!broadcast(new JoinNotification(user))) {
									System.err.println("broadcastPacketQueue ist voll, JoinNotification wurde verworfen");
								}
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
							// Alle Clients mit der aktuellen Benutzerliste aktualisieren
							broadcastUserList();
						}
					}
					case RegisterRequest req -> authHandler.handleRegister(req, sender);
					// group action packets — only logged in clients can use these
					case CreateGroupPacket cgp -> {
						if (sender != null && sender.getUser() != null) {
							Group created = groupManager.createGroup(cgp.getGroupName(), sender);
							sender.tryEnqueuePacket(new GroupCreatedPacket(created));
						}
					}
					case JoinGroupPacket jgp -> {
						if (sender != null && sender.getUser() != null) {
							groupManager.joinGroup(jgp.getGroupId(), sender);
							Group joined = groupManager.getGroup(jgp.getGroupId());
							if (joined != null) sender.tryEnqueuePacket(new GroupCreatedPacket(joined));
						}
					}
					case LeaveGroupPacket lgp -> {
						if (sender != null && sender.getUser() != null)
							groupManager.leaveGroup(lgp.getGroupId(), sender);
					}
					case AddMemberPacket amp -> {
						if (sender != null && sender.getUser() != null) {
							if (!groupManager.isMember(amp.getGroupId(), sender)) break;
							if (!userRepository.usernameExists(amp.getUsername())) break;
							Map<String, ClientProxy> online = new HashMap<>();
							synchronized (clients) {
								for (var c : clients) {
									if (c.getUser() != null) online.put(c.getUser().getUsername(), c);
								}
							}
							if (groupManager.addMemberByUsername(amp.getGroupId(), amp.getUsername(), online)) {
								ClientProxy added = online.get(amp.getUsername());
								if (added != null) {
									added.tryEnqueuePacket(new GroupListResponsePacket(groupManager.getGroupsForClient(added)));
								}
							}
						}
					}
					case GroupListRequestPacket ignored -> {
						if (sender != null)
							sender.tryEnqueuePacket(new GroupListResponsePacket(groupManager.getAllGroups()));
					}
					case MyGroupsRequestPacket ignored -> {
						if (sender != null)
							sender.tryEnqueuePacket(new GroupListResponsePacket(groupManager.getGroupsForClient(sender)));
					}
					case GetGroupMembersRequest req -> {
						if (sender != null) {
							Set<String> members = groupManager.getMemberUsernames(req.getGroupId());
							Group g = groupManager.getGroup(req.getGroupId());
							String creator = g != null ? g.getCreatorUsername() : null;
							sender.tryEnqueuePacket(new GetGroupMembersResponse(req.getGroupId(), members, creator));
						}
					}
					case RemoveMemberPacket rmp -> {
						if (sender != null && sender.getUser() != null) {
							Group group = groupManager.getGroup(rmp.getGroupId());
							// Creator kann nicht entfernt werden
							if (group != null && rmp.getUsername().equals(group.getCreatorUsername())) break;
							Map<String, ClientProxy> online = new HashMap<>();
							synchronized (clients) {
								for (var c : clients) {
									if (c.getUser() != null) online.put(c.getUser().getUsername(), c);
								}
							}
							String groupName = group != null ? group.getName() : "Unbekannte Gruppe";
							ClientProxy removed = groupManager.removeMemberByUsername(rmp.getGroupId(), rmp.getUsername(), online);
							if (removed != null) {
								String removedBy = sender.getUser().getDisplayname() != null && !sender.getUser().getDisplayname().isBlank()
									? sender.getUser().getDisplayname() : sender.getUser().getUsername();
								removed.tryEnqueuePacket(new GroupRemovedNotification(rmp.getGroupId(), groupName, removedBy));
							}
						}
					}
					case GetUsersRequest ignored -> {
						if (sender != null && sender.getUser() != null) {
							try {
								// Einfache Kopien ohne Hibernate-Proxies erstellen
								List<User> simple = userRepository.getAllUsers().stream().map(u -> {
									User copy = new User(u.getUsername());
									copy.setDisplayname(u.getDisplayname());
									return copy;
								}).collect(Collectors.toList());
								sender.tryEnqueuePacket(new GetUsersResponse(simple));
							} catch (Exception e) {
								System.err.println("Fehler beim Laden aller Benutzer: " + e);
								e.printStackTrace();
							}
						}
					}
					case FileMessage file -> {
						if (sender != null && sender.getUser() != null) {
							try {
								UUID fileId = FileUtil.saveFile(file.getContent(), file.getFileExtension());
								Long dbId = saveHistoryEntry(file, fileId.toString());
								FileMessage withDbId = dbId != null
									? new FileMessage(file.getSender(), file.getContent(), file.getFileExtension(), dbId, file.getSentAt())
									: new FileMessage(file.getSender(), file.getContent(), file.getFileExtension());
								withDbId.setGroupId(file.getGroupId());
								withDbId.setReceiverUsername(file.getReceiverUsername());
								routeMessage(withDbId);
							} catch (IOException e) {
								System.err.println("Fehler beim Speichern einer Datei: " + e);
							}
						}
					}
					case TextMessage textMessage -> {
						if (sender != null && sender.getUser() != null) {
							String content = textMessage.getContent();

							if (content != null && content.startsWith("/ai ")) {
								User botUser = new User();
								botUser.setUsername("KI-Assistent");

								String prompt = content.substring(4).trim();
								if (prompt.isEmpty()) {
									sender.tryEnqueuePacket(new TextMessage(botUser, "Bitte gib nach /ai noch eine Frage ein."));
									break;
								}

								threadExecutor.submit(() -> {
									try {
										String answer = geminiHandler.ask(prompt);

										if (answer == null || answer.isBlank()) {
											sender.tryEnqueuePacket(new TextMessage(botUser, "Stell deine Frage erneut."));
											return;
										}

										sender.tryEnqueuePacket(new TextMessage(botUser, answer));

									} catch (Exception e) {
										sender.tryEnqueuePacket(new TextMessage(botUser, "Fehler beim Abruf des KI-Assistenten."));
									}
								});

							} else {
								Long dbId = saveHistoryEntry(textMessage, null);
								TextMessage withDbId = dbId != null
									? new TextMessage(textMessage.getSender(), textMessage.getContent(), dbId, textMessage.getSentAt())
									: new TextMessage(textMessage.getSender(), textMessage.getContent());
								withDbId.setGroupId(textMessage.getGroupId());
								withDbId.setReceiverUsername(textMessage.getReceiverUsername());
								routeMessage(withDbId);
							}
						}
					}

					case Message msg -> {
						if (sender != null && sender.getUser() != null) {
							routeMessage(msg);
						}
					}
					case ReadReceipt receipt -> broadcastToAll(packet);
					case EditMessage edit -> broadcastToAll(packet);
					case DeleteForMeMessage deleteForMe -> {
						if (sender != null && sender.getUser() != null) {
							try {
								deletedForUserRepository.save(deleteForMe.getMessageId(), sender.getUser().getUsername());
							} catch (Exception e) {
								System.err.println("Fehler beim Speichern von 'nur für mich löschen': " + e.getMessage());
							}
						}
					}
					case DeleteMessage delete -> {
						chatHistoryService.markAsDeleted(delete.getMessageId());
						broadcastToAll(packet);
					}
					case HistoryRequest histReq -> {
						if (sender != null && sender.getUser() != null) {
							histReq.setSender(sender.getUser().getUsername());
						}
						chatHistoryHandler.handleHistoryRequest(histReq, sender);
					}
					//Audio
					case Util.Network.Notifications.CallNotification call -> {
						if (sender != null) {
							call.setSenderIp(sender.getIpAddress());
						}
						broadcastToAll(call);
					}
					case Util.Network.ProfilePictureUpdate update -> {
						Util.Network.ProfilePictureUpdate saved = authHandler.handleProfilePictureUpdate(update, sender);
						if (saved != null) {
							broadcastToAll(saved);
						}
					}

					default -> broadcastToAll(packet);
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		closeAllClients();
	}

	// Routes a message to group, private, or global chat based on message fields.
	private void routeMessage(Message msg) {
		if (msg.getGroupId() != null) {
			for (var member : groupManager.getGroupMembers(msg.getGroupId())) {
				member.tryEnqueuePacket(msg);
			}
		} else if (msg.getReceiverUsername() != null) {
			// Private message: deliver to sender and receiver only
			synchronized (clients) {
				for (var client : clients) {
					User u = client.getUser();
					if (u != null && (u.getUsername().equals(msg.getReceiverUsername())
							|| (msg.getSender() != null && u.getUsername().equals(msg.getSender().getUsername())))) {
						client.tryEnqueuePacket(msg);
					}
				}
			}
		} else {
			broadcastToAll(msg);
		}
	}

	private void broadcastToAll(Packet packet) {
		ArrayList<ClientProxy> clientsToUnregister = new ArrayList<>();
		int broadcastCount = 0;
		int skippedCount = 0;

		synchronized (clients) {
			for (var client : clients) {
				if (client.shouldStop()) {
					clientsToUnregister.add(client);
				} else if (client.getUser() == null) {
					skippedCount++;
				} else if (!client.tryEnqueuePacket(packet)) {
					System.out.println("     └─ Client outPacketQueue ist voll!");
					clientsToUnregister.add(client);
				} else {
					broadcastCount++;
				}
			}
		}


		for (var client : clientsToUnregister) {
			if (!unregister(client)) {
				System.err.println("Zu entfernenden Client nicht gefunden");
			}
		}

		for (var client : clientsToUnregister) {
			User user = client.getUser();
			// todo: Benutzernamen des Clients übergeben oder keine Benachrichtigung senden wenn nicht eingeloggt
			if (user == null) {
				user = new User();
				user.setUsername("Platzhalter");
			}

			try {
				if (!broadcast(new LeaveNotification(user))) {
					System.err.println("broadcastPacketQueue ist voll, Paket wurde verworfen");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/// Fügt einen neuen Client zur Liste der verbundenen Clients hinzu.
	/// Gibt `true` zurück, wenn der Client erfolgreich registriert wurde.
	public boolean register(SocketProxy socket) {
		if (stopFlag.get()) {
			return false;
		}

		ArrayList<ClientProxy> clientsToUnregister = new ArrayList<>();

		synchronized (clients) {
			if (clients.size() >= MAX_CLIENTS) {
				collectClientsToUnregister(clientsToUnregister);

				if (clientsToUnregister.isEmpty()) {
					return false;
				} else {
					System.err.println("Maximale Anzahl an Clients erreicht, es werden " + clientsToUnregister.size() + " inaktive Clients entfernt");
					for (var client : clientsToUnregister) {
						if (!unregister(client)) {
							System.err.println("Zu entfernenden Client nicht gefunden");
						}
					}
					clientsToUnregister.clear();
				}
			}
		}

		BlockingQueue<Packet> outPacketQueue = new ArrayBlockingQueue<>(MAX_OUTGOING_PACKETS);
		var client = new ClientProxy(socket, broadcastPacketQueue, outPacketQueue, threadExecutor);

		synchronized (clients) {
			clients.add(client);
		}

		return true;
	}

	/// Entfernt einen Client aus der Liste der verbundenen Clients und schließt die Verbindung.
	/// Gibt `true` zurück, wenn der Client erfolgreich entfernt wurde.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean unregister(ClientProxy client) {
		boolean removed;

		synchronized (clients) {
			removed = clients.remove(client);
		}

		if (removed) {
			groupManager.unregisterClient(client);
			try {
				client.close();
			} catch (IOException e) {
				System.err.println("Fehler beim Schließen eines Clients: " + e);
			}
		}

		return removed;
	}

	/// Fügt ein Paket zur Broadcast-Queue hinzu, damit es an alle verbundenen Clients gesendet wird.
	/// Gibt `true` zurück, wenn das Paket erfolgreich zur Queue hinzugefügt wurde.
	public boolean broadcast(Packet packet) throws InterruptedException {
		if (stopFlag.get()) {
			return true;
		}

		return broadcastPacketQueue.offer(new IncomingPacket(packet, null));
	}

	private void broadcastUserList() {
		try {
			List<User> simple = userRepository.getAllUsers().stream().map(u -> {
				User copy = new User(u.getUsername());
				copy.setDisplayname(u.getDisplayname());
				return copy;
			}).collect(Collectors.toList());
			broadcastToAll(new GetUsersResponse(simple));
		} catch (Exception e) {
			System.err.println("Fehler beim Broadcasten der Benutzerliste: " + e.getMessage());
		}
	}

	public void shutdown() {
		stopFlag.set(true);
	}

	/// Sammelt alle Clients, die unregistriert werden müssen, in der übergebenen Liste.
	/// Es muss die Clients-Liste synchronisiert werden, bevor diese Methode aufgerufen wird.
	private void collectClientsToUnregister(List<ClientProxy> clientsToUnregister) {
		for (var client : clients) {
			if (client.shouldStop()) {
				clientsToUnregister.add(client);
			}
		}
	}

	private void closeAllClients() {
		ArrayList<ClientProxy> clientsToClose;

		synchronized (clients) {
			clientsToClose = new ArrayList<>(clients);
			clients.clear();
		}

		for (var client : clientsToClose) {
			try {
				client.close();
			} catch (IOException e) {
				System.err.println("Fehler beim Schließen eines Clients: " + e);
			}
		}
	}

	private void cleanupDisconnectedClients() {
		ArrayList<ClientProxy> clientsToUnregister = new ArrayList<>();

		synchronized (clients) {
			for (var client : clients) {
				if (client.shouldStop()) {
					clientsToUnregister.add(client);
				}
			}
		}

		for (var client : clientsToUnregister) {
			if (!unregister(client)) {
				System.err.println("Zu entfernenden Client nicht gefunden");
			}
		}
	}

	private Long saveHistoryEntry(Message message, String filePath) {
		User sender = message.getSender();
		if (sender == null || sender.getUsername() == null || sender.getUsername().isBlank()) {
			System.err.println("Nachricht hat keinen Sender, wird nicht gespeichert");
			return null;
		}

		String content;
		MessageType messageType;
		if (message instanceof TextMessage textMessage) {
			content = textMessage.getContent();
			messageType = MessageType.TEXT;
		} else if (message instanceof FileMessage fileMessage) {
			content = "[Datei: " + fileMessage.getFileExtension() + "]";
			messageType = MessageType.FILE;
		} else {
			return null;
		}

		String receiver = message.getReceiverUsername();
		String chatRoomId;
		if (message.getGroupId() != null) {
			chatRoomId = message.getGroupId().toString();
			receiver = null;
		} else if (receiver != null) {
			chatRoomId = null;
		} else {
			chatRoomId = "main";
		}

		ChatMessage dbMessage = new ChatMessage(
			sender.getUsername(),
			receiver,
			chatRoomId,
			content,
			messageType,
			filePath
		);
		try {
			chatHistoryService.saveMessage(dbMessage);
			return dbMessage.getId();
		} catch (RuntimeException e) {
			System.err.println("Fehler beim Speichern der Chat-History: " + e.getMessage());
			return null;
		}
	}
}
