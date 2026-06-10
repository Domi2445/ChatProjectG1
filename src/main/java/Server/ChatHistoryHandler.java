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

			HistoryResponse response = new HistoryResponse(dbMessages);
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

