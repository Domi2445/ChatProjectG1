package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ControllerRegister {

	@FXML
	private TextField usernameField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Button registerButton;

	private Stage stage;

	@FXML
	private void initialize() {
		registerButton.setOnAction(e -> handleRegister());
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	private void handleRegister() {
		String username = usernameField.getText().trim();
		String password = passwordField.getText();

		if (validateInput(username, password)) {
			System.out.println("Registrierungsversuch für: " + username);
			// TODO: Registrierungs-Anfrage an den Server senden
		}
	}

	private boolean validateInput(String username, String password) {
		if (username.isEmpty()) {
			showError("Benutzername ist erforderlich");
			return false;
		}
		if (password.isEmpty()) {
			showError("Passwort ist erforderlich");
			return false;
		}
		return true;
	}

	private void showError(String message) {
		System.err.println("Fehler: " + message);
		// TODO: Fehlermeldung in der UI anzeigen
	}
}

