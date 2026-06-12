package User.Repository;

import User.Model.ChatMessage;
import User.Model.MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import User.Repository.RepositoryException;



public class ChatHistoryService {

	private final ChatMessageRepository repository;
	private final Map<String, List<ChatMessage>> cache; // Cache für Verläufe, keyed by chatKey (z.B. "sender-receiver" oder "chatRoomId")

	public ChatHistoryService() {
		this.repository = new ChatMessageRepository();
		this.cache = new ConcurrentHashMap<>();
	}


	public void saveMessage(ChatMessage message) {
		try {
			repository.saveMessage(message);
			// Cache invalidieren für den betroffenen Chat
			String chatKey = getChatKey(message.getSender(), message.getReceiver(), message.getChatRoomId());
			cache.remove(chatKey);
		} catch (RepositoryException e) {
			System.err.println("Fehler beim Speichern der Nachricht: " + e.getMessage());
			e.printStackTrace();
			// Hier könntest du weitere Fehlerbehandlung hinzufügen, z.B. Logging
		}
	}


	public List<ChatMessage> getHistory(String sender, String receiver, String chatRoomId) {
		String chatKey = getChatKey(sender, receiver, chatRoomId);
		try {
			List<ChatMessage> history = cache.get(chatKey);
			if (history == null) {
				history = repository.getHistoryForChat(sender, receiver, chatRoomId);
				cache.put(chatKey, history);
			}
			return history;
		} catch (RepositoryException e) {
			System.err.println("Fehler beim Laden des Chat-Verlaufs: " + e.getMessage());
			e.printStackTrace();
			return List.of(); // Leere Liste bei Fehler
		}
	}


	public void exportHistoryToFile(String sender, String receiver, String chatRoomId, String fileName) {
		try {
			List<ChatMessage> history = getHistory(sender, receiver, chatRoomId);
			Path exportDir = Paths.get("chat_exports");
			Files.createDirectories(exportDir);
			Path filePath = exportDir.resolve(fileName + ".txt");

			StringBuilder content = new StringBuilder();
			content.append("Chat-Verlauf exportiert am ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

			for (ChatMessage msg : history) {
				content.append("[").append(msg.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("] ")
					.append(msg.getSender()).append(": ");
				if (msg.getMessageType() == MessageType.FILE) {
					content.append("[Datei: ").append(msg.getFilePath() != null ? msg.getFilePath() : "Unbekannt").append("]");
				} else {
					content.append(msg.getContent());
				}
				content.append("\n");
			}

			Files.writeString(filePath, content.toString());
			System.out.println("Verlauf exportiert nach: " + filePath.toAbsolutePath());
		} catch (IOException e) {
			System.err.println("Fehler beim Exportieren des Verlaufs: " + e.getMessage());
			e.printStackTrace();
		} catch (RepositoryException e) {
			System.err.println("Fehler beim Laden des Verlaufs für Export: " + e.getMessage());
			e.printStackTrace();
		}
	}


	public void markAsDeleted(long messageId) {
		try {
			repository.markAsDeleted(messageId);
			cache.clear();
		} catch (RepositoryException e) {
			System.err.println("Fehler beim Markieren der Nachricht als gelöscht: " + e.getMessage());
		}
	}

	public int deleteOldMessages(int daysOld) {
		try {
			int deleted = repository.deleteOldMessages(daysOld);
			// Cache leeren, da alte Daten entfernt wurden
			cache.clear();
			return deleted;
		} catch (RepositoryException e) {
			System.err.println("Fehler beim Löschen alter Nachrichten: " + e.getMessage());
			e.printStackTrace();
			return 0;
		}
	}


	private String getChatKey(String sender, String receiver, String chatRoomId) {
		if (chatRoomId != null) {
			return "room:" + chatRoomId;
		} else {
			// Sortiere Sender und Receiver für konsistenten Schlüssel
			return sender.compareTo(receiver) < 0 ? sender + "-" + receiver : receiver + "-" + sender;
		}
	}
}
