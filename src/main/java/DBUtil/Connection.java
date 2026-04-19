package DBUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
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
					entityManagerFactory = local = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, buildPersistenceOverrides());
				}
			}
		}
		return local;
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

		String dbUrl = normalize(firstNonBlank(System.getProperty("db.url"), System.getenv("DB_URL")));
		String dbUser = normalize(firstNonBlank(System.getProperty("db.user"), System.getenv("DB_USER")));
		String dbPassword = normalize(firstNonBlank(System.getProperty("db.password"), System.getenv("DB_PASSWORD")));

		// Platzhalter/leer => lokaler H2-Fallback.
		if (isBlank(dbUrl) || dbUrl.contains("${")) {
			applyH2Defaults(overrides);
			return overrides;
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

	/**
	 * Setzt lokale H2-Defaults.
	 *
	 * <p>Die DB ist dateibasiert im Projektordner unter {@code ./data/chatdb}.
	 * {@code MODE=Oracle} reduziert SQL-Unterschiede waehrend der Entwicklung.
	 *
	 * @param overrides Ziel-Map fuer Properties
	 */
	private static void applyH2Defaults(Map<String, Object> overrides) {
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
