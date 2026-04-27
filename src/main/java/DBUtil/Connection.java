package DBUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zentrale Fabrik/Verwaltung für JPA-Zugriffe.
 *
 * <p>Die Klasse baut eine singletonartige {@link EntityManagerFactory} auf Basis
 * der Persistence-Unit {@code chat-oracle} und dynamischer Overrides auf.
 *
 * <p>Konfigurationsquellen (Prioritize):
 * <ol>
 *   <li>Java System Properties (z. B. {@code -Ddb.url=...})</li>
 *   <li>Umgebungsvariablen (z. B. {@code DB_URL})</li>
 *   <li>Fallback-Defaults (lokal: H2)</li>
 * </ol>
 *
 * <p>Unterstuetzte Parameter:
 * <ul>
 *   <li>{@code db.url} / {@code DB_URL}</li>
 *   <li>{@code db.user} / {@code DB_USER}</li>
 *   <li>{@code db.password} / {@code DB_PASSWORD}</li>
 *   <li>{@code db.ddl} / {@code DB_DDL} (z. B. update, validate)</li>
 *   <li>{@code db.showSql} / {@code DB_SHOW_SQL}</li>
 *   <li>{@code db.formatSql} / {@code DB_FORMAT_SQL}</li>
 * </ul>
 */
public final class Connection {

	/** Name der Persistence-Unit aus {@code persistence.xml}. */
	private static final String PERSISTENCE_UNIT = "chat-oracle";

	/**
	 * Lazy initialisierte, threadsichere EMF-Instanz.
	 * {@code volatile} ist fuer korrektes Double-Checked-Locking notwendig.
	 */
	private static volatile EntityManagerFactory entityManagerFactory;
	private static volatile boolean triedOracleFallback;
	private static volatile String activeDatabaseLabel = "uninitialisiert";

	static {
		// Schließt die EMF beim JVM-Shutdown sauber.
		Runtime.getRuntime().addShutdownHook(new Thread(Connection::shutdown, "JpaShutdownHook"));
	}

	/** Utility-Klasse: keine Instanzierung erlaubt. */
	private Connection() {
	}

	/**
	 * Liefert die globale {@link EntityManagerFactory}.
	 *
	 * <p>Initialisiert die Factory lazy und threadsicher per Double-Checked-Locking.
	 *
	 * @return offene EntityManagerFactory
	 */
	public static EntityManagerFactory getEntityManagerFactory() {
		EntityManagerFactory local = entityManagerFactory;
		if (local == null || !local.isOpen()) {
			synchronized (Connection.class) {
				local = entityManagerFactory;
				if (local == null || !local.isOpen()) {
					Map<String, Object> overrides = buildPersistenceOverrides();
					try {
						setActiveDatabaseLabel(overrides);
						entityManagerFactory = local = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, overrides);
						System.out.println("Datenbank verbunden: " + activeDatabaseLabel);
					} catch (RuntimeException firstFailure) {
						if (!shouldRetryWithH2(overrides) || triedOracleFallback) {
							throw firstFailure;
						}

						triedOracleFallback = true;
						System.err.println("Oracle-Verbindung fehlgeschlagen, wechsle auf H2: " + firstFailure.getMessage());
						Map<String, Object> h2Overrides = buildH2Overrides();
						setActiveDatabaseLabel(h2Overrides);
						entityManagerFactory = local = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, h2Overrides);
						System.out.println("Datenbank verbunden: " + activeDatabaseLabel);
					}
				}
			}
		}
		return local;
	}

	public static String getActiveDatabaseLabel() {
		getEntityManagerFactory();
		return activeDatabaseLabel;
	}

	/**
	 * Erzeugt einen neuen {@link EntityManager} aus der globalen Factory.
	 *
	 * @return neuer EntityManager
	 */
	public static EntityManager createEntityManager() {
		return getEntityManagerFactory().createEntityManager();
	}

	/**
	 * Schließt die globale EntityManagerFactory, falls sie offen ist.
	 *
	 * <p>Synchronisiert, damit parallele Aufrufe keinen inkonsistenten Zustand erzeugen.
	 */
	public static synchronized void shutdown() {
		if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}

	/**
	 * Baut dynamische JPA/Hibernate-Overrides fuer den Factory-Start.
	 *
	 * <p>Entscheidung:
	 * <ul>
	 *   <li>Kein gueltiger {@code db.url} gesetzt -> H2-Defaults</li>
	 *   <li>Oracle-URL erkannt -> Oracle-Treiber + Dialekt, Credentials erforderlich</li>
	 *   <li>Sonst -> H2-Treiber + H2-Dialekt mit ggf. Standard-Credentials</li>
	 * </ul>
	 *
	 * @return Map mit Persistenz-Properties fuer {@code createEntityManagerFactory(...)}
	 * @throws IllegalStateException wenn Oracle-URL vorhanden, aber User/Passwort fehlen
	 */
	private static Map<String, Object> buildPersistenceOverrides() {
		Map<String, Object> overrides = new HashMap<>();

		String dbUrl = normalize(firstNonBlank(System.getProperty("db.url"), System.getenv("DB_URL"), readEnvFileValue("DB_URL")));
		String dbUser = normalize(firstNonBlank(System.getProperty("db.user"), System.getenv("DB_USER"), readEnvFileValue("DB_USER")));
		String dbPassword = normalize(firstNonBlank(System.getProperty("db.password"), System.getenv("DB_PASSWORD"), readEnvFileValue("DB_PASSWORD")));

		if (isBlank(dbUrl) || dbUrl.contains("${")) {
			return buildH2Overrides();
		}

		if (isOracleUrl(dbUrl)) {
			// Oracle explizit: Zugangsdaten sind Pflicht.
			if (isBlank(dbUser) || isBlank(dbPassword)) {
				throw new IllegalStateException(
					"Oracle-Zugangsdaten fehlen oder sind Platzhalter. Setze DB_URL, DB_USER und DB_PASSWORD " +
						"(oder -Ddb.url/-Ddb.user/-Ddb.password)."
				);
			}
			overrides.put("jakarta.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
			overrides.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
		} else {
			// Nicht-Oracle-URL: auf H2 verhalten (inkl. Standard-Credentials).
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

		// DDL-Strategie: zur Laufzeit uebersteuerbar, sonst Default "update".
		overrides.put("hibernate.hbm2ddl.auto", firstNonBlank(System.getProperty("db.ddl"), System.getenv("DB_DDL"), "update"));

		// SQL-Logging nur setzen, wenn explizit angegeben.
		String showSql = firstNonBlank(System.getProperty("db.showSql"), System.getenv("DB_SHOW_SQL"));
		if (!isBlank(showSql)) {
			overrides.put("hibernate.show_sql", showSql);
		}

		String formatSql = firstNonBlank(System.getProperty("db.formatSql"), System.getenv("DB_FORMAT_SQL"));
		if (!isBlank(formatSql)) {
			overrides.put("hibernate.format_sql", formatSql);
		}
		return overrides;
	}

	private static Map<String, Object> buildH2Overrides() {
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
		overrides.put("jakarta.persistence.jdbc.url", "jdbc:h2:./data/chatdb;MODE=Oracle;AUTO_SERVER=TRUE");
		overrides.put("jakarta.persistence.jdbc.user", "sa");
		overrides.put("jakarta.persistence.jdbc.password", "");
		overrides.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		overrides.put("hibernate.hbm2ddl.auto", firstNonBlank(System.getProperty("db.ddl"), System.getenv("DB_DDL"), "update"));

		// Falls nicht gesetzt: SQL-Logging standardmaessig aus.
		String showSql = firstNonBlank(System.getProperty("db.showSql"), System.getenv("DB_SHOW_SQL"));
		if (!isBlank(showSql)) {
			overrides.put("hibernate.show_sql", showSql);
		} else {
			overrides.put("hibernate.show_sql", "false");
		}

		String formatSql = firstNonBlank(System.getProperty("db.formatSql"), System.getenv("DB_FORMAT_SQL"));
		if (!isBlank(formatSql)) {
			overrides.put("hibernate.format_sql", formatSql);
		} else {
			overrides.put("hibernate.format_sql", "false");
		}

		return overrides;
	}

	private static void setActiveDatabaseLabel(Map<String, Object> overrides) {
		Object url = overrides.get("jakarta.persistence.jdbc.url");
		if (url instanceof String urlString && isOracleUrl(urlString)) {
			activeDatabaseLabel = "Oracle";
		} else {
			activeDatabaseLabel = "H2";
		}
	}

	private static boolean shouldRetryWithH2(Map<String, Object> overrides) {
		Object url = overrides.get("jakarta.persistence.jdbc.url");
		return url instanceof String urlString && isOracleUrl(urlString);
	}

	private static String readEnvFileValue(String key) {
		for (Path candidate : candidateEnvFiles()) {
			if (!Files.isRegularFile(candidate)) {
				continue;
			}

			try {
				for (String line : Files.readAllLines(candidate)) {
					String trimmed = line.trim();
					if (trimmed.isEmpty() || trimmed.startsWith("#")) {
						continue;
					}

					int separator = trimmed.indexOf('=');
					if (separator <= 0) {
						continue;
					}

					String candidateKey = trimmed.substring(0, separator).trim();
					if (candidateKey.equals(key)) {
						return trimmed.substring(separator + 1).trim();
					}
				}
			} catch (IOException ignored) {
				// Keine .env vorhanden oder nicht lesbar -> normale Env-/Property-Werte werden genutzt.
			}
		}
		return null;
	}

	private static List<Path> candidateEnvFiles() {
		Path cwd = Path.of("").toAbsolutePath().normalize();
		Path parent = cwd.getParent();
		Path grandParent = parent != null ? parent.getParent() : null;
		return List.of(
			cwd.resolve(".env"),
			parent != null ? parent.resolve(".env") : cwd.resolve(".env"),
			grandParent != null ? grandParent.resolve(".env") : cwd.resolve(".env")
		);
	}

	/**
	 * Einfache Oracle-Erkennung anhand typischer JDBC-Muster.
	 *
	 * @param dbUrl JDBC-URL
	 * @return {@code true}, wenn URL wie Oracle aussieht
	 */
	private static boolean isOracleUrl(String dbUrl) {
		String lower = dbUrl.toLowerCase();
		return lower.contains("oracle") || lower.contains(":thin:");
	}

	/**
	 * Gibt den ersten nicht-leeren String aus der Kandidatenliste zurueck.
	 *
	 * @param candidates moegliche Werte
	 * @return erster nicht-blank Wert oder {@code null}
	 */
	private static String firstNonBlank(String... candidates) {
		for (String candidate : candidates) {
			if (!isBlank(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Trimmt einen Wert und wandelt blanke Strings in {@code null} um.
	 *
	 * @param value Rohwert
	 * @return getrimmter Wert oder {@code null}
	 */
	private static String normalize(String value) {
		return isBlank(value) ? null : value.trim();
	}

	/**
	 * Prueft, ob ein String {@code null}, leer oder nur Whitespace ist.
	 *
	 * @param value zu pruefender String
	 * @return {@code true}, wenn blank
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
