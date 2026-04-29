package Client;

import User.Login.Status;
import Util.Network.Auth.RegisterRequest;
import Util.Network.Auth.RegisterResponse;
import Util.Network.SocketProxy;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

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
			sendRegisterRequest(username, username, password);
		}
	}

	private void sendRegisterRequest(String username, String displayname, String password) {
		Thread t = new Thread(() -> {
			try (SocketProxy socket = new SocketProxy(new Socket("127.0.0.1", 6969))) {
				socket.getOutputStream().writeObject(new RegisterRequest(username, displayname, password));
				socket.getOutputStream().flush();

				while (true) {
					Object packet = socket.getInputStream().readObject();
					if (packet instanceof RegisterResponse response) {
						Platform.runLater(() -> showRegisterResult(response));
						return;
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				Platform.runLater(() -> showError("Verbindung zum Server fehlgeschlagen: " + e.getMessage()));
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private void showRegisterResult(RegisterResponse response) {
		if (response.getStatus() == Status.SUCCESS) {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Registrierung erfolgreich");
			alert.setContentText(response.getMessage());
			alert.show();
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Registrierung fehlgeschlagen");
			alert.setContentText(response.getMessage());
			alert.show();
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
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("Fehler");
		alert.setContentText(message);
		alert.show();
	}
}
