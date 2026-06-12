package Server;

import User.Model.ChatMessage;
import User.Repository.ChatMessageRepository;
import User.Repository.UserRepository;
import Util.Network.ProfilePictureUpdate;
import Util.Network.HistoryRequest;
import Util.Network.HistoryResponse;

import java.util.List;
import java.util.Collections;

/**
 * Handler die HistoryRequest vom Client verarbeitet.
 * Ruft das Repository auf und sendet die gefundenen Messages als HistoryResponse zurück.
 */
public class ChatHistoryHandler {
	private final ChatMessageRepository messageRepository;
	private final UserRepository userRepository;

	public ChatHistoryHandler(ChatMessageRepository messageRepository, UserRepository userRepository) {
		this.messageRepository = messageRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Verarbeitet einen HistoryRequest vom Client.
	 * Holt die Chat-History aus der DB und antwortet mit HistoryResponse.
	 */
	public void handleHistoryRequest(HistoryRequest request, ClientProxy clientProxy) {
		try {
			String sender = request.getSender();
			String receiver = request.getReceiver();
			String chatRoomId = request.getChatRoomId();

			List<ChatMessage> dbMessages = messageRepository.getHistoryForChat(sender, receiver, chatRoomId);

			// Sammle alle Dateiinhalte, die in der History referenziert werden
			java.util.Map<String, byte[]> fileContents = new java.util.HashMap<>();
			java.util.Map<String, String> fileExtensions = new java.util.HashMap<>();
			try {
				String tmp = System.getProperty("java.io.tmpdir");
				if (tmp != null) {
					java.nio.file.Path dir = java.nio.file.Path.of(tmp).resolve("ChatProjektG1-Bilder");
					if (java.nio.file.Files.exists(dir) && java.nio.file.Files.isDirectory(dir)) {
						for (ChatMessage m : dbMessages) {
							if (m.getMessageType() != null && m.getMessageType().name().equals("FILE")) {
								String fileId = m.getFilePath();
								if (fileId == null || fileId.isBlank()) continue;
								try {
									java.util.UUID uuid = java.util.UUID.fromString(fileId);
									// Suche in directory nach Datei mit dem uuid-Präfix
									try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(dir)) {
										java.util.Optional<java.nio.file.Path> found = s.filter(p -> p.getFileName().toString().startsWith(uuid.toString())).findFirst();
										if (found.isPresent()) {
											java.nio.file.Path fp = found.get();
											byte[] data = java.nio.file.Files.readAllBytes(fp);
											fileContents.put(fileId, data);
											String name = fp.getFileName().toString();
											int dot = name.lastIndexOf('.');
											String ext = dot == -1 ? "bin" : name.substring(dot + 1).toLowerCase();
											fileExtensions.put(fileId, ext);
										}
									}
								} catch (Exception ignored) {
									// falls UUID ungültig oder Lesen fehlschlägt -> überspringen
								}
							}
						}
					}
				}
			} catch (Exception ignored) {
			}

			HistoryResponse response = new HistoryResponse(dbMessages, fileContents, fileExtensions);
			clientProxy.tryEnqueuePacket(response);

			try {
				java.util.Set<String> uniqueSenders = new java.util.HashSet<>();
				for (ChatMessage m : dbMessages) {
					if (m.getSender() != null) uniqueSenders.add(m.getSender());
				}
				for (String senderUsername : uniqueSenders) {
					try {
						var userOpt = userRepository.findByUsername(senderUsername);
						if (userOpt.isPresent()) {
							var user = userOpt.get();
							byte[] img = user.getProfilePicture();
							String ct = user.getProfilePictureContentType();
							if (img != null && img.length > 0 && ct != null) {
								clientProxy.tryEnqueuePacket(new ProfilePictureUpdate(senderUsername, img, ct));
							}
						}
					} catch (Exception ignored) {
						// Falls einzelne User nicht geladen werden können -> weiter
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception e) {
			System.err.println("Fehler beim Verarbeiten von HistoryRequest: " + e.getMessage());
			e.printStackTrace();
			try {
				HistoryResponse errorResponse = new HistoryResponse(Collections.emptyList(), "error", e.getMessage());
				clientProxy.tryEnqueuePacket(errorResponse);
			} catch (Exception ex) {
				System.err.println("Fehler beim Senden der Error-Response: " + ex.getMessage());
			}
		}
	}
}

