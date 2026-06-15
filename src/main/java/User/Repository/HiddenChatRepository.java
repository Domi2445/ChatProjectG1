package User.Repository;

import DBUtil.Connection;
import User.Model.HiddenChat;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class HiddenChatRepository {

    public void hide(String username, String chatRef) {
        try (EntityManager em = Connection.createEntityManager()) {
            long count = em.createQuery(
                    "SELECT COUNT(h) FROM HiddenChat h WHERE h.username = :u AND h.chatRef = :r", Long.class)
                .setParameter("u", username).setParameter("r", chatRef)
                .getSingleResult();
            if (count > 0) return;
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(new HiddenChat(username, chatRef));
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                System.err.println("HiddenChat speichern fehlgeschlagen: " + e.getMessage());
            }
        }
    }

    public List<String> getHiddenRefs(String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            return em.createQuery(
                "SELECT h.chatRef FROM HiddenChat h WHERE h.username = :u", String.class)
                .setParameter("u", username)
                .getResultList();
        }
    }
}
