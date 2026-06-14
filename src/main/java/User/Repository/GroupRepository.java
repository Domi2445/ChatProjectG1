package User.Repository;

import DBUtil.Connection;
import User.Model.ChatGroup;
import User.Model.GroupMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.UUID;

public class GroupRepository {

    public void saveGroup(UUID id, String name, String creatorUsername) {
        // ignore if already exists (e.g. broadcast group on every startup)
        try (EntityManager em = Connection.createEntityManager()) {
            if (em.find(ChatGroup.class, id.toString()) != null) return;
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(new ChatGroup(id, name, creatorUsername));
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
            }
        }
    }

    public void addMember(UUID groupId, String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(new GroupMember(groupId.toString(), username));
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                // unique constraint = already member, ignore
            }
        }
    }

    public List<ChatGroup> getAllGroups() {
        try (EntityManager em = Connection.createEntityManager()) {
            return em.createQuery("SELECT g FROM ChatGroup g", ChatGroup.class).getResultList();
        }
    }

    public List<String> getGroupIdsForUser(String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            return em.createQuery(
                "SELECT m.groupId FROM GroupMember m WHERE m.username = :u", String.class)
                .setParameter("u", username)
                .getResultList();
        }
    }
}
