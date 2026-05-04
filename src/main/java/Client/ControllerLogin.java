package Client;

import User.Login.Status;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.LoginResponse;
import Util.Network.SocketProxy;
import javafx.application.Platform;
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
import java.net.Socket;

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
			sendLoginRequest(username, password);
		}
	}

	private void sendLoginRequest(String username, String password) {
		Thread t = new Thread(() -> {
			try (SocketProxy socket = new SocketProxy(new Socket("127.0.0.1", 6969))) {
				socket.getOutputStream().writeObject(new LoginRequest(username, password));
				socket.getOutputStream().flush();

				while (true) {
					Object packet = socket.getInputStream().readObject();
					if (packet instanceof LoginResponse response) {
						Platform.runLater(() -> showLoginResult(response));
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

	private void showLoginResult(LoginResponse response) {
		if (response.getStatus() == Status.SUCCESS) {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Login erfolgreich");
			alert.setContentText(response.getMessage());
			alert.show();
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Login fehlgeschlagen");
			alert.setContentText(response.getMessage());
			alert.show();
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
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("Fehler");
		alert.setContentText(message);
		alert.show();
	}
}
