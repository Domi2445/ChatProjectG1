package User.Repository;

import DBUtil.Connection;
import User.Model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import java.util.Optional;

public class JPAUserRepository implements UserRepository {

	@Override
	public Optional<User> findByUsername(String username) {
		validateUsername(username);

		try (EntityManager entityManager = Connection.createEntityManager()) {
			User user = entityManager.createQuery(
					"SELECT u FROM User u WHERE u.username = :username", User.class)
				.setParameter("username", username)
				.getSingleResult();
			return Optional.of(user);
		} catch (NoResultException e) {
			return Optional.empty();
		} catch (NonUniqueResultException e) {
			throw new RepositoryException("Mehrere User mit gleichem Username gefunden: " + username, e);
		} catch (RuntimeException e) {
			throw new RepositoryException("User konnte nicht geladen werden: " + username, e);
		}
	}

	@Override
	public void createUser(User user) {
		validateUser(user);

		try {
			executeInTransaction(entityManager -> {
				if (entityManager.find(User.class, user.getUsername()) != null) {
					throw new UsernameAlreadyExistsException(user.getUsername());
				}
				entityManager.persist(user);
			}, "User konnte nicht erstellt werden: " + user.getUsername());
		} catch (RepositoryException e) {
			// Bei paralleler Registrierung trotzdem fachliche Exception liefern.
			try {
				if (usernameExists(user.getUsername())) {
					throw new UsernameAlreadyExistsException(user.getUsername());
				}
			} catch (RepositoryException ignored) {
				// Falls der Re-Check technisch scheitert, Originalfehler beibehalten.
			}
			throw e;
		}
	}

	@Override
	public void updateUser(User user) {
		validateUser(user);

		executeInTransaction(entityManager -> {
			User existing = entityManager.find(User.class, user.getUsername());
			if (existing == null) {
				throw new UserNotFoundException(user.getUsername());
			}
			entityManager.merge(user);
		}, "User konnte nicht aktualisiert werden: " + user.getUsername());
	}

	@Override
	public void deleteUser(String username) {
		validateUsername(username);

		executeInTransaction(entityManager -> {
			User user = entityManager.find(User.class, username);
			if (user == null) {
				throw new UserNotFoundException(username);
			}
			entityManager.remove(user);
		}, "User konnte nicht gelöscht werden: " + username);
	}

	@Override
	public boolean usernameExists(String username) {
		validateUsername(username);

		try (EntityManager entityManager = Connection.createEntityManager()) {
			Long count = entityManager.createQuery(
					"SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
				.setParameter("username", username)
				.getSingleResult();
			return count != null && count > 0;
		} catch (RuntimeException e) {
			throw new RepositoryException("Username-Prüfung fehlgeschlagen: " + username, e);
		}
	}

	private void executeInTransaction(EntityWork work, String errorMessage) {
		try (EntityManager entityManager = Connection.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			try {
				transaction.begin();
				work.run(entityManager);
				transaction.commit();
			} catch (RuntimeException e) {
				rollbackQuietly(transaction);
				throw translateRuntimeException(errorMessage, e);
			}
		}
	}

	private RuntimeException translateRuntimeException(String errorMessage, RuntimeException e) {
		if (e instanceof UsernameAlreadyExistsException || e instanceof UserNotFoundException || e instanceof IllegalArgumentException || e instanceof RepositoryException) {
			return e;
		}
		return new RepositoryException(errorMessage, e);
	}

	private void rollbackQuietly(EntityTransaction transaction) {
		if (transaction != null && transaction.isActive()) {
			transaction.rollback();
		}
	}

	private void validateUser(User user) {
		if (user == null) {
			throw new IllegalArgumentException("User darf nicht null sein");
		}
		validateUsername(user.getUsername());
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private void validateUsername(String username) {
		if (isBlank(username)) {
			throw new IllegalArgumentException("Username darf nicht null oder leer sein");
		}
	}

	@FunctionalInterface
	private interface EntityWork {
		void run(EntityManager entityManager);
	}
}
