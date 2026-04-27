package Server;

import User.Login.Status;
import User.Model.User;
import User.Repository.JPAUserRepository;
import User.Repository.RepositoryException;
import User.Repository.UserRepository;
import User.Repository.UsernameAlreadyExistsException;
import Util.FileUtil;
import Util.Login.BCryptWrapper;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.LoginResponse;
import Util.Network.Auth.RegisterRequest;
import Util.Network.Auth.RegisterResponse;
import Util.Network.Groups.CreateGroupPacket;
import Util.Network.Groups.JoinGroupPacket;
import Util.Network.Groups.LeaveGroupPacket;
import Util.Network.Messages.FileMessage;
import Util.Network.Messages.Message;
import Util.Network.Notifications.LeaveNotification;
import Util.Network.Notifications.JoinNotification;
import Util.Network.Packet;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/// Verteilt Pakete an alle angemeldeten Clients.
public class PacketBroker implements Runnable {
	public static final int MAX_INCOMING_PACKETS = 128;
	public static final int MAX_OUTGOING_PACKETS = 128;
	public static final int MAX_CLIENTS = 16;

	private final ExecutorService threadExecutor;
	private final GroupManager groupManager;
	private final UserRepository userRepository;

	/// Queue für Pakete, die an alle verbundenen Clients gesendet werden sollen.
	private final BlockingQueue<PacketCarrier> broadcastPacketQueue;
	/// Liste aller aktuell verbundenen Clients.
	private final List<ClientProxy> clients;
	private final AtomicBoolean stopFlag;

	public PacketBroker(ExecutorService threadExecutor) {
		this.threadExecutor = threadExecutor;
		this.groupManager = new GroupManager();
		this.userRepository = new JPAUserRepository();

		this.broadcastPacketQueue = new ArrayBlockingQueue<>(MAX_INCOMING_PACKETS);
		this.clients = new ArrayList<>(MAX_CLIENTS);
		this.stopFlag = new AtomicBoolean(false);
	}

	@Override
	public void run() {
		ArrayList<ClientProxy> clientsToUnregister = new ArrayList<>();

		while (!stopFlag.get() && !Thread.currentThread().isInterrupted()) {
			try {
				PacketCarrier carrier = broadcastPacketQueue.take();
				Packet packet = carrier.packet;
				ClientProxy sender = carrier.sender;

				// ---- auth ----
				if (packet instanceof LoginRequest lr) {
					handleLogin(lr, sender);
					continue;
				}
				if (packet instanceof RegisterRequest rr) {
					handleRegister(rr, sender);
					continue;
				}

				// ---- group actions ----
				if (packet instanceof CreateGroupPacket cgp) {
					handleCreateGroup(cgp, sender);
					continue;
				}
				if (packet instanceof JoinGroupPacket jgp) {
					handleJoinGroup(jgp, sender);
					continue;
				}
				if (packet instanceof LeaveGroupPacket lgp) {
					handleLeaveGroup(lgp, sender);
					continue;
				}

				if (packet instanceof FileMessage file) {
					try {
						FileUtil.saveFile(file.getContent(), file.getFileExtension());
					} catch (IOException e) {
						System.err.println("Fehler beim Speichern einer Datei: " + e);
						continue;
					}
				}

				// ---- routing ----
				if (packet instanceof Message msg && msg.getGroupId() != null) {
					for (var member : groupManager.getGroupMembers(msg.getGroupId())) {
						if (!member.tryEnqueuePacket(msg)) {
							System.err.println("Client outPacketQueue ist voll");
							clientsToUnregister.add(member);
						}
					}
				} else {
					synchronized (clients) {
						for (var client : clients) {
							if (client.shouldStop()) {
								clientsToUnregister.add(client);
							} else if (!client.tryEnqueuePacket(packet)) {
								System.err.println("Client outPacketQueue ist voll");
								clientsToUnregister.add(client);
							}
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

					if (!broadcast(new LeaveNotification(user))) {
						System.err.println("broadcastPacketQueue ist voll, Paket wurde verworfen");
					}
				}

				clientsToUnregister.clear();

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		closeAllClients();
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
	public boolean broadcast(Packet packet) {
		if (stopFlag.get()) {
			return true;
		}

		return broadcastPacketQueue.offer(new PacketCarrier(packet, null));
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

	private void handleLogin(LoginRequest request, ClientProxy sender) {
		if (sender == null) return;
		try {
			Optional<User> optUser = userRepository.findByUsername(request.getUsername());
			if (optUser.isEmpty() || !BCryptWrapper.validate(request.getPassword(), optUser.get().getPasswordHash())) {
				sender.tryEnqueuePacket(new LoginResponse(Status.WRONG_CREDENTIALS, "Username oder Passwort falsch.", null));
				return;
			}
			User user = optUser.get();
			sender.setUser(user);
			groupManager.registerClient(sender, user);
			sender.tryEnqueuePacket(new LoginResponse(Status.SUCCESS, "", user));
			broadcast(new JoinNotification(user));
			System.out.println("Eingeloggt: " + user.getUsername());
		} catch (RepositoryException e) {
			System.err.println("Datenbankfehler beim Login: " + e);
			sender.tryEnqueuePacket(new LoginResponse(Status.DATABASE_ERROR, "Datenbankfehler.", null));
		}
	}

	private void handleRegister(RegisterRequest request, ClientProxy sender) {
		if (sender == null) return;
		try {
			User newUser = new User();
			newUser.setUsername(request.getUsername());
			newUser.setDisplayname(request.getDisplayname());
			newUser.setPasswordHash(BCryptWrapper.hash(request.getPassword()));
			userRepository.createUser(newUser);
			sender.tryEnqueuePacket(new RegisterResponse(Status.SUCCESS, "Registrierung erfolgreich."));
		} catch (UsernameAlreadyExistsException e) {
			sender.tryEnqueuePacket(new RegisterResponse(Status.USERNAME_TAKEN, "Benutzername bereits vergeben."));
		} catch (RepositoryException e) {
			System.err.println("Datenbankfehler bei Registrierung: " + e);
			sender.tryEnqueuePacket(new RegisterResponse(Status.DATABASE_ERROR, "Datenbankfehler."));
		}
	}

	private void handleCreateGroup(CreateGroupPacket packet, ClientProxy sender) {
		if (sender == null || sender.getUser() == null) return;
		groupManager.createGroup(packet.getGroupName(), sender);
	}

	private void handleJoinGroup(JoinGroupPacket packet, ClientProxy sender) {
		if (sender == null || sender.getUser() == null) return;
		groupManager.joinGroup(packet.getGroupId(), sender);
	}

	private void handleLeaveGroup(LeaveGroupPacket packet, ClientProxy sender) {
		if (sender == null || sender.getUser() == null) return;
		groupManager.leaveGroup(packet.getGroupId(), sender);
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
}
