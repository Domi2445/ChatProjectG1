package User.Repository;

import User.Model.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Persistence;

import java.util.NoSuchElementException;
import java.util.Optional;

public class JPAUserRepository implements UserRepository {

    private EntityManagerFactory emf;
    private EntityManager entityManager;

    public JPAUserRepository() {
        this.emf = Persistence.createEntityManagerFactory("chat-oracle");
        this.entityManager = emf.createEntityManager();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username darf nicht null sein");
        }

        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();

            return Optional.of(user);
        } catch (NoResultException e)
        {
            return Optional.empty();
        } catch (NonUniqueResultException e)
        {
            throw new IllegalStateException("Mehrere User mit gleichem Username gefunden");
        }
    }

    @Override
    public void createUser(User user)
    {
        if (user == null || user.getUsername() == null)
        {
            throw new IllegalArgumentException("User oder Username darf nicht null sein");
        }

        if (usernameExists(user.getUsername()))
        {
            throw new IllegalArgumentException("Username existiert bereits");
        }

        entityManager.getTransaction().begin();
        entityManager.persist(user);
        entityManager.getTransaction().commit();
    }

    @Override
    public void updateUser(User user)
    {
        if (user == null || user.getUsername() == null)
        {
            throw new IllegalArgumentException("User oder Username darf nicht null sein");
        }

        // Prüfen ob genau dieser User existiert
        User existing = entityManager.find(User.class, user.getUsername());
        if (existing == null) {
            throw new NoSuchElementException("User existiert nicht");
        }

        entityManager.getTransaction().begin();
        entityManager.merge(user);
        entityManager.getTransaction().commit();
    }

    @Override
    public void deleteUser(String username)
    {
        if (username == null)
        {
            throw new IllegalArgumentException("Username darf nicht null sein");
        }

        // robuster als Query → direkt über Primary Key
        User user = entityManager.find(User.class, username);
        if (user == null)
        {
            throw new NoSuchElementException("User existiert nicht");
        }

        entityManager.getTransaction().begin();
        entityManager.remove(user);
        entityManager.getTransaction().commit();
    }

    //
    @Override
    public boolean usernameExists(String username)
    {
        if (username == null)
        {
            throw new IllegalArgumentException("Username darf nicht null sein");
        }

        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();

        return count > 0;
    }
}