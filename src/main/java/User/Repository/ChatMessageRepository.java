package User.Repository;

import DBUtil.Connection;
import User.Model.ChatMessage;
import User.Model.MessageType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageRepository {

	public void saveMessage(ChatMessage message) {
		try (EntityManager entityManager = Connection.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			try {
				transaction.begin();
				entityManager.persist(message);
				transaction.commit();
			} catch (RuntimeException e) {
				if (transaction.isActive()) {
					transaction.rollback();
				}
				throw new RepositoryException("Nachricht konnte nicht gespeichert werden", e);
			}
		}
	}

	public List<ChatMessage> getHistoryForChat(String sender, String receiver, String chatRoomId) {
		try (EntityManager entityManager = Connection.createEntityManager()) {
			String queryStr;
			if (chatRoomId != null) {
				// Gruppenchat
				queryStr = "SELECT m FROM ChatMessage m WHERE m.chatRoomId = :chatRoomId ORDER BY m.timestamp";
				return entityManager.createQuery(queryStr, ChatMessage.class)
					.setParameter("chatRoomId", chatRoomId)
					.getResultList();
			} else {
				// Privater Chat
				queryStr = "SELECT m FROM ChatMessage m WHERE " +
					"((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1)) " +
					"ORDER BY m.timestamp";
				return entityManager.createQuery(queryStr, ChatMessage.class)
					.setParameter("user1", sender)
					.setParameter("user2", receiver)
					.getResultList();
			}
		} catch (RuntimeException e) {
			throw new RepositoryException("Chat-Verlauf konnte nicht geladen werden", e);
		}
	}

	// Neue Methode: Löscht Nachrichten, die älter als die angegebene Anzahl von Tagen sind
	public int deleteOldMessages(int daysOld) {
		try (EntityManager entityManager = Connection.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			try {
				transaction.begin();
				LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
				String queryStr = "DELETE FROM ChatMessage m WHERE m.timestamp < :cutoffDate";
				int deletedCount = entityManager.createQuery(queryStr)
					.setParameter("cutoffDate", cutoffDate)
					.executeUpdate();
				transaction.commit();
				return deletedCount; // Gibt die Anzahl der gelöschten Nachrichten zurück
			} catch (RuntimeException e) {
				if (transaction.isActive()) {
					transaction.rollback();
				}
				throw new RepositoryException("Alte Nachrichten konnten nicht gelöscht werden", e);
			}
		}
	}

	// Optionale weitere Methode: Zählt die Anzahl der Nachrichten in einem Chat
	public long getMessageCountForChat(String sender, String receiver, String chatRoomId) {
		try (EntityManager entityManager = Connection.createEntityManager()) {
			String queryStr;
			if (chatRoomId != null) {
				// Gruppenchat
				queryStr = "SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoomId = :chatRoomId";
				return entityManager.createQuery(queryStr, Long.class)
					.setParameter("chatRoomId", chatRoomId)
					.getSingleResult();
			} else {
				// Privater Chat
				queryStr = "SELECT COUNT(m) FROM ChatMessage m WHERE " +
					"((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1))";
				return entityManager.createQuery(queryStr, Long.class)
					.setParameter("user1", sender)
					.setParameter("user2", receiver)
					.getSingleResult();
			}
		} catch (RuntimeException e) {
			throw new RepositoryException("Nachrichtenanzahl konnte nicht ermittelt werden", e);
		}
	}
}
