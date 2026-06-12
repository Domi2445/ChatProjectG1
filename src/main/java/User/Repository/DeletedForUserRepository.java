package User.Repository;

import DBUtil.Connection;
import User.Model.DeletedForUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class DeletedForUserRepository {

	public void save(long messageId, String username) {
		try (EntityManager em = Connection.createEntityManager()) {
			EntityTransaction tx = em.getTransaction();
			try {
				tx.begin();
				em.persist(new DeletedForUser(messageId, username));
				tx.commit();
			} catch (RuntimeException e) {
				if (tx.isActive()) tx.rollback();
				throw new RepositoryException("Konnte gelöschte Nachricht nicht speichern", e);
			}
		}
	}

	public List<Long> getDeletedMessageIdsForUser(String username) {
		try (EntityManager em = Connection.createEntityManager()) {
			return em.createQuery("SELECT d.messageId FROM DeletedForUser d WHERE d.username = :username", Long.class)
				.setParameter("username", username)
				.getResultList();
		} catch (RuntimeException e) {
			throw new RepositoryException("Konnte gelöschte Nachrichten nicht laden", e);
		}
	}
}
