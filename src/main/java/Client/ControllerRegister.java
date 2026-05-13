package Client;

import User.Login.Status;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ControllerRegister {

	@FXML
	private TextField textFieldUsername;

	@FXML
	private PasswordField textFieldPassword;

	@FXML
	private Button buttonRegister;

	@FXML
	private Button buttonBack;

	private Stage stage;
	private Controller controller;
	private Scene chatScene;
	private Scene loginScene;

	@FXML
	private void initialize() {
		buttonRegister.setOnAction(e -> {
			buttonRegister.setDisable(true);
			handleRegister();
		});
		buttonBack.setOnAction(e -> {
			stage.setTitle("Socket Chat - Login");
			stage.setScene(loginScene);
		});
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

	public void setLoginScene(Scene loginScene) {
		this.loginScene = loginScene;
	}

	private void handleRegister() {
		String username = textFieldUsername.getText().trim();
		String password = textFieldPassword.getText();

		if (!validateInput(username, password)) return;

		controller.setOnRegisterResult(response -> {
			if (response.getStatus() == Status.SUCCESS) {
				stage.setTitle("Socket Chat");
				stage.setScene(chatScene);
			} else {
				buttonRegister.setDisable(false);
				showError(response.getMessage());
			}
		});

		controller.sendRegisterRequest(username, username, password);
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
