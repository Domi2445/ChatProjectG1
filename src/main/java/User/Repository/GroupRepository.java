package User.Repository;

import DBUtil.Connection;
import User.Model.ChatGroup;
import User.Model.GroupMember;
import User.Model.RemovedGroupMember;
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
                System.err.println("saveGroup fehlgeschlagen (" + id + "): " + e.getMessage());
            }
        }
    }

    public void addMember(UUID groupId, String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            String gid = groupId.toString();
            long count = em.createQuery(
                    "SELECT COUNT(m) FROM GroupMember m WHERE m.groupId = :g AND m.username = :u", Long.class)
                .setParameter("g", gid).setParameter("u", username)
                .getSingleResult();
            if (count > 0) return;
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(new GroupMember(gid, username));
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                System.err.println("addMember fehlgeschlagen (" + groupId + ", " + username + "): " + e.getMessage());
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

    public List<String> getMembersForGroup(String groupId) {
        try (EntityManager em = Connection.createEntityManager()) {
            return em.createQuery(
                "SELECT m.username FROM GroupMember m WHERE m.groupId = :g", String.class)
                .setParameter("g", groupId)
                .getResultList();
        }
    }

    public void markAsRemoved(UUID groupId, String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(new RemovedGroupMember(groupId.toString(), username));
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                // unique constraint = already marked, ignore
            }
        }
    }

    public List<String> getRemovedGroupIdsForUser(String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            return em.createQuery(
                "SELECT r.groupId FROM RemovedGroupMember r WHERE r.username = :u", String.class)
                .setParameter("u", username)
                .getResultList();
        }
    }

    public void removeMember(UUID groupId, String username) {
        try (EntityManager em = Connection.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.createQuery("DELETE FROM GroupMember m WHERE m.groupId = :g AND m.username = :u")
                    .setParameter("g", groupId.toString())
                    .setParameter("u", username)
                    .executeUpdate();
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
            }
        }
    }
}
