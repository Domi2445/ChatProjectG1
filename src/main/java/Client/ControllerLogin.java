package Client;

import User.Login.Status;
import User.Model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
	private Controller chatController;

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
			try {
				// Chat-View laden und Controller vorbereiten
				FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/Client/chat-view.fxml"));
				Parent chatRoot = chatLoader.load();

				chatController = chatLoader.getController();

				// Callback für Login-Response registrieren
				chatController.setOnLoginResult(response -> {
					if (response.getStatus() == Status.SUCCESS) {
						User user = response.getUser();
						chatController.configure(stage, user);

						// Zur Chat-View wechseln
						Scene chatScene = new Scene(chatRoot, 1280, 720);
						stage.setTitle("Socket Chat");
						stage.setScene(chatScene);

						// Mit Server verbinden
						chatController.connectAndRun("127.0.0.1", 6969);
					} else {
						showError("Login fehlgeschlagen: " + response.getStatus());
					}
				});

				// Login-Request senden
				chatController.sendLoginRequest(username, password);

			} catch (IOException ex) {
				showError("Fehler beim Laden der Chat-View: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	private void handleRegisterClick() {
		try {
			FXMLLoader registerLoader = new FXMLLoader(getClass().getResource("/Client/registerScreen.fxml"));
			Parent registerRoot = registerLoader.load();

			ControllerRegister registerController = registerLoader.getController();
			registerController.setStage(stage);
			registerController.setLoginController(this);

			Scene scene = new Scene(registerRoot, 1280, 720);
			stage.setTitle("Socket Chat - Registrierung");
			stage.setScene(scene);
		} catch (IOException e) {
			showError("Fehler beim Laden des Registrierungsbildschirms: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void goBackToLogin() {
		try {
			FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/Client/loginScreen.fxml"));
			Parent loginRoot = loginLoader.load();

			ControllerLogin newLoginController = loginLoader.getController();
			newLoginController.setStage(stage);

			Scene scene = new Scene(loginRoot, 1280, 720);
			stage.setTitle("Socket Chat - Login");
			stage.setScene(scene);
		} catch (IOException e) {
			showError("Fehler beim Laden des Login-Bildschirms: " + e.getMessage());
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
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Fehler");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
