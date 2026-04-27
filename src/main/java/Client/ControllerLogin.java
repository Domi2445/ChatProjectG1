package Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerLogin {

	@FXML
	private TextField usernameField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Button loginButton;

	@FXML
	private Label registerLink;

	private Stage stage;

	@FXML
	private void initialize() {
		loginButton.setOnAction(e -> handleLogin());
		registerLink.setOnMouseClicked(e -> handleRegisterClick());
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	private void handleLogin() {
		String username = usernameField.getText().trim();
		String password = passwordField.getText();

		if (validateInput(username, password)) {
			System.out.println("Login-Versuch für: " + username);
			// TODO: Login-Anfrage an den Server senden
		}
	}

	private void handleRegisterClick() {
		try {
			FXMLLoader registerLoader = new FXMLLoader(getClass().getResource("/Client/registerScreen.fxml"));
			Parent registerRoot = registerLoader.load();

			ControllerRegister registerController = registerLoader.getController();
			registerController.setStage(stage);

			Scene scene = new Scene(registerRoot, 1280, 720);
			stage.setTitle("Socket Chat - Registrierung");
			stage.setScene(scene);
		} catch (IOException e) {
			System.err.println("Fehler beim Laden des Registrierungsbildschirms: " + e.getMessage());
			e.printStackTrace();
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
