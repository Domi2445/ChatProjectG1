package Server;

import User.Model.ChatMessage;
import User.Repository.ChatMessageRepository;
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

	public ChatHistoryHandler(ChatMessageRepository messageRepository) {
		this.messageRepository = messageRepository;
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

			System.out.println("  📨 HistoryRequest erhalten: sender=" + sender + ", receiver=" + receiver + ", chatRoomId=" + chatRoomId);

			List<ChatMessage> dbMessages = messageRepository.getHistoryForChat(sender, receiver, chatRoomId);

			System.out.println("    🔍 Gefundene Messages in DB: " + (dbMessages != null ? dbMessages.size() : 0));
			if (dbMessages != null && !dbMessages.isEmpty()) {
				for (ChatMessage msg : dbMessages) {
					System.out.println("      └─ " + msg.getSender() + ": " + msg.getContent());
				}
			}

			HistoryResponse response = new HistoryResponse(dbMessages);
			clientProxy.tryEnqueuePacket(response);

			String chatIdentifier = chatRoomId != null
					? "'" + chatRoomId + "'"
					: (sender != null || receiver != null)
						? "zwischen '" + sender + "' und '" + receiver + "'"
						: "alle Nachrichten";
			System.out.println("✓ History für Chat " + chatIdentifier + " (" + (dbMessages != null ? dbMessages.size() : 0) + " Messages) gesendet");
		} catch (Exception e) {
			System.err.println("❌ Fehler beim Verarbeiten von HistoryRequest: " + e.getMessage());
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

