package DBUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class Connection {

	private static final String PERSISTENCE_UNIT = "chat-oracle";
	private static volatile EntityManagerFactory entityManagerFactory;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(Connection::shutdown, "JpaShutdownHook"));
	}

	private Connection() {
	}

	public static EntityManagerFactory getEntityManagerFactory() {
		EntityManagerFactory local = entityManagerFactory;
		if (local == null || !local.isOpen()) {
			synchronized (Connection.class) {
				local = entityManagerFactory;
				if (local == null || !local.isOpen()) {
					entityManagerFactory = local = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, buildPersistenceOverrides());
				}
			}
		}
		return local;
	}

	public static EntityManager createEntityManager() {
		return getEntityManagerFactory().createEntityManager();
	}

	public static synchronized void shutdown() {
		if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}

	private static Map<String, Object> buildPersistenceOverrides() {
		Map<String, Object> overrides = new HashMap<>();

		String dbUrl = normalize(firstNonBlank(System.getProperty("db.url"), System.getenv("DB_URL")));
		String dbUser = normalize(firstNonBlank(System.getProperty("db.user"), System.getenv("DB_USER")));
		String dbPassword = normalize(firstNonBlank(System.getProperty("db.password"), System.getenv("DB_PASSWORD")));

		if (isBlank(dbUrl) || dbUrl.contains("${")) {
			applyH2Defaults(overrides);
			return overrides;
		}

		if (isOracleUrl(dbUrl)) {
			if (isBlank(dbUser) || isBlank(dbPassword)) {
				throw new IllegalStateException(
					"Oracle-Zugangsdaten fehlen oder sind Platzhalter. Setze DB_URL, DB_USER und DB_PASSWORD " +
						"(oder -Ddb.url/-Ddb.user/-Ddb.password)."
				);
			}
			overrides.put("jakarta.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
			overrides.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
		} else {
			overrides.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
			overrides.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			if (isBlank(dbUser)) {
				dbUser = "sa";
			}
			if (isBlank(dbPassword)) {
				dbPassword = "";
			}
		}

		overrides.put("jakarta.persistence.jdbc.url", dbUrl);
		overrides.put("jakarta.persistence.jdbc.user", dbUser);
		overrides.put("jakarta.persistence.jdbc.password", dbPassword);
		overrides.put("hibernate.hbm2ddl.auto", firstNonBlank(System.getProperty("db.ddl"), System.getenv("DB_DDL"), "update"));
		overrides.put("hibernate.show_sql", firstNonBlank(System.getProperty("db.showSql"), System.getenv("DB_SHOW_SQL"), "true"));
		overrides.put("hibernate.format_sql", firstNonBlank(System.getProperty("db.formatSql"), System.getenv("DB_FORMAT_SQL"), "true"));
		return overrides;
	}

	private static void applyH2Defaults(Map<String, Object> overrides) {
		overrides.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
		overrides.put("jakarta.persistence.jdbc.url", "jdbc:h2:./data/chatdb;MODE=Oracle;AUTO_SERVER=TRUE");
		overrides.put("jakarta.persistence.jdbc.user", "sa");
		overrides.put("jakarta.persistence.jdbc.password", "");
		overrides.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		overrides.put("hibernate.hbm2ddl.auto", "update");
		overrides.put("hibernate.show_sql", "true");
		overrides.put("hibernate.format_sql", "true");
	}

	private static boolean isOracleUrl(String dbUrl) {
		String lower = dbUrl.toLowerCase();
		return lower.contains("oracle") || lower.contains(":thin:");
	}

	private static String firstNonBlank(String... candidates) {
		for (String candidate : candidates) {
			if (!isBlank(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static String normalize(String value) {
		return isBlank(value) ? null : value.trim();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
