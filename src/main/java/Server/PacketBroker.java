package Server;

import User.Model.ChatMessage;
import User.Model.MessageType;
import User.Model.User;
import User.Repository.ChatHistoryService;
import User.Repository.ChatMessageRepository;
import User.Repository.JPAUserRepository;
import Util.FileUtil;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.RegisterRequest;
import Util.Network.DeleteMessage;
import Util.Network.EditMessage;
import Util.Network.Groups.CreateGroupPacket;
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
import Util.Network.ReadReceipt;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
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
	private final GroupManager groupManager;

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
		this.groupManager = new GroupManager();

		this.broadcastPacketQueue = new ArrayBlockingQueue<>(MAX_INCOMING_PACKETS);
		this.clients = new ArrayList<>(MAX_CLIENTS);
		this.stopFlag = new AtomicBoolean(false);
	}

	@Override
	public void run() {
		while (!stopFlag.get() && !Thread.currentThread().isInterrupted()) {
			try {
				cleanupDisconnectedClients();
				IncomingPacket incoming = broadcastPacketQueue.take();
				Packet packet = incoming.packet();
				ClientProxy sender = incoming.sender();

				switch (packet) {
					case LoginRequest req -> {
						if (authHandler.handleLogin(req, sender)) {
							User user = sender.getUser();
							// register with group manager so this client can create/join groups
							groupManager.registerClient(sender, user);

							// History direkt nach Login senden — zu diesem Zeitpunkt sind
							// alle vorher verarbeiteten Nachrichten bereits in der DB.
							chatHistoryHandler.handleHistoryRequest(new HistoryRequest(null, null, "main"), sender);

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
						}
					}
					case RegisterRequest req -> authHandler.handleRegister(req, sender);
					// group action packets — only logged in clients can use these
					case CreateGroupPacket cgp -> {
						if (sender != null && sender.getUser() != null)
							groupManager.createGroup(cgp.getGroupName(), sender);
					}
					case JoinGroupPacket jgp -> {
						if (sender != null && sender.getUser() != null)
							groupManager.joinGroup(jgp.getGroupId(), sender);
					}
					case LeaveGroupPacket lgp -> {
						if (sender != null && sender.getUser() != null)
							groupManager.leaveGroup(lgp.getGroupId(), sender);
					}
					case GroupListRequestPacket ignored -> {
						if (sender != null)
							sender.tryEnqueuePacket(new GroupListResponsePacket(groupManager.getAllGroups()));
					}
					case MyGroupsRequestPacket ignored -> {
						if (sender != null)
							sender.tryEnqueuePacket(new GroupListResponsePacket(groupManager.getGroupsForClient(sender)));
					}
					case FileMessage file -> {
						if (sender != null && sender.getUser() != null) {
							try {
								UUID fileId = FileUtil.saveFile(file.getContent(), file.getFileExtension());
								saveHistoryEntry(file, fileId.toString());
								broadcastToAll(packet);
							} catch (IOException e) {
								System.err.println("Fehler beim Speichern einer Datei: " + e);
							}
						}
					}
					case TextMessage textMessage -> {
						if (sender != null && sender.getUser() != null) {
							String content = textMessage.getContent();
							int contentLength = content != null ? content.length() : 0;
							boolean isAiCommand = content != null && content.startsWith("/ai ");

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
								saveHistoryEntry(textMessage, null);
								routeMessage(textMessage);
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
					case DeleteMessage delete -> broadcastToAll(packet);
					case HistoryRequest histReq -> chatHistoryHandler.handleHistoryRequest(histReq, sender);
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

	// if the message has a group id, only send to group members. otherwise broadcast to everyone.
	private void routeMessage(Message msg) {
		if (msg.getGroupId() != null) {
			for (var member : groupManager.getGroupMembers(msg.getGroupId())) {
				member.tryEnqueuePacket(msg);
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

	private void saveHistoryEntry(Message message, String filePath) {
		User sender = message.getSender();
		if (sender == null || sender.getUsername() == null || sender.getUsername().isBlank()) {
			System.err.println("Nachricht hat keinen Sender, wird nicht gespeichert");
			return;
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
			return;
		}

		ChatMessage dbMessage = new ChatMessage(
			sender.getUsername(),
			null,
			"main",  // Standard-Chatroom für globale Nachrichten
			content,
			messageType,
			filePath
		);
		try {
			chatHistoryService.saveMessage(dbMessage);
		} catch (RuntimeException e) {
			System.err.println("Fehler beim Speichern der Chat-History: " + e.getMessage());
		}
	}
}
