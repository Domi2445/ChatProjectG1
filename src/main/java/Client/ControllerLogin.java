package Client;

import User.Login.Status;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
	private Controller controller;
	private Scene chatScene;

	@FXML
	private void initialize() {
		loginButton.setOnAction(e -> {
			loginButton.setDisable(true);
			handleLogin();
		});
		registerLink.setOnMouseClicked(e -> handleRegisterClick());
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public void setChatScene(Scene chatScene) {
		this.chatScene = chatScene;
	}

	private void handleLogin() {
		String username = usernameField.getText().trim();
		String password = passwordField.getText();

		if (!validateInput(username, password)) return;

		controller.setOnLoginResult(response -> {
			if (response.getStatus() == Status.SUCCESS) {
				stage.setTitle("Socket Chat");
				stage.setScene(chatScene);
			} else {
				loginButton.setDisable(false);
				showError(response.getMessage());
			}
		});

		controller.sendLoginRequest(username, password);
	}

	private void handleRegisterClick() {
		try {
			FXMLLoader registerLoader = new FXMLLoader(getClass().getResource("/Client/registerScreen.fxml"));
			Parent registerRoot = registerLoader.load();

			ControllerRegister registerController = registerLoader.getController();
			registerController.setStage(stage);
			registerController.setController(controller);
			registerController.setChatScene(chatScene);

			stage.setTitle("Socket Chat - Registrierung");
			stage.setScene(new Scene(registerRoot, 1280, 720));
		} catch (IOException e) {
			showError("Fehler beim Laden des Registrierungsbildschirms: " + e.getMessage());
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
		alert.setHeaderText("Fehler");
		alert.setContentText(message);
		alert.show();
	}
}
