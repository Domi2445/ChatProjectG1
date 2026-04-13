package DBUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Connection
{

	// Name of the persistence unit defined in persistence.xml
	private static final String PERSISTENCE_UNIT_NAME = "chat-oracle";
	private static final EntityManagerFactory ENTITY_MANAGER_FACTORY =
			Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

	private Connection()
	{
	}

	public static EntityManager createEntityManager() {
		return ENTITY_MANAGER_FACTORY.createEntityManager();
	}

	public static void closeFactory() {
		if (ENTITY_MANAGER_FACTORY.isOpen()) {
			ENTITY_MANAGER_FACTORY.close();
		}
	}
}
