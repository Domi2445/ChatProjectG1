package User.Repository;

/**
 * Einheitliche technische Ausnahme fuer Repository-Zugriffe.
 */
public class RepositoryException extends RuntimeException {

	public RepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RepositoryException(String message) {
		super(message);
	}
}

