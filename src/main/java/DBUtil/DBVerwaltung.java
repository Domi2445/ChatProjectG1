package DBUtil;

import User.Model.User;
import User.Repository.JPAUserRepository;
import User.Repository.RepositoryException;
import User.Repository.UserRepository;
import User.Repository.UsernameAlreadyExistsException;
import Util.Login.BCryptWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
* Dient als Starter für die Datenbankverwaltung, um User z. B. anzulegen, bzw. auch den Admin anzulegen.
 * Auch zum Verwalten der DB anstatt der Test-DB.
 * Hier können vom Admin auch User gelöscht werden. usw.
 */
public class DBVerwaltung {
	private final UserRepository userRepository;

	@FXML
	private TextField usernameField;

	@FXML
	private TextField displaynameField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Label statusLabel;

	public DBVerwaltung() {
		this.userRepository = new JPAUserRepository();
	}

	@FXML
	private void onCreateUser() {
		String username = readTrimmed(usernameField);
		String displayname = readTrimmed(displaynameField);
		String password = passwordField.getText();

		if (username.isEmpty()) {
			showStatus("Username darf nicht leer sein.", false);
			return;
		}

		if (password == null || password.isBlank()) {
			showStatus("Passwort darf nicht leer sein.", false);
			return;
		}

		if (displayname.isEmpty()) {
			displayname = username;
		}

		User user = new User();
		user.setUsername(username);
		user.setDisplayname(displayname);
		user.setPasswordHash(BCryptWrapper.hash(password));

		try {
			userRepository.createUser(user);
			passwordField.clear();
			showStatus("User '" + username + "' wurde erstellt.", true);
		} catch (UsernameAlreadyExistsException e) {
			showStatus("Username bereits vergeben: " + username, false);
		} catch (IllegalArgumentException e) {
			showStatus("Ungültige Eingabe: " + e.getMessage(), false);
		} catch (RepositoryException e) {
			System.err.println("Fehler beim Erstellen des Users '" + username + "': " + e.getMessage());
			e.printStackTrace(System.err);
			String detail = e.getMessage();
			showStatus(
				detail == null || detail.isBlank()
					? "Datenbankfehler beim Erstellen des Users."
					: "Datenbankfehler beim Erstellen des Users: " + detail,
				false
			);
		}
	}

	private String readTrimmed(TextField field) {
		String value = field.getText();
		return value == null ? "" : value.trim();
	}

	private void showStatus(String message, boolean success) {
		statusLabel.setText(message);
		statusLabel.setStyle(success ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #c62828;");
	}
}
