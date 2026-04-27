	package Client;

	import User.Login.Status;
	import javafx.fxml.FXML;
	import javafx.scene.control.Alert;
	import javafx.scene.control.Button;
	import javafx.scene.control.Label;
	import javafx.scene.control.PasswordField;
	import javafx.scene.control.TextField;
	import javafx.stage.Stage;

	public class ControllerRegister {

		@FXML
		private TextField usernameField;

		@FXML
		private TextField displaynameField;

		@FXML
		private PasswordField passwordField;

		@FXML
		private Button registerButton;

		@FXML
		private Label backLink;

		private Stage stage;
		private ControllerLogin loginController;
		private Controller chatController;

		@FXML
		private void initialize() {
			registerButton.setOnAction(e -> handleRegister());
			if (backLink != null) {
				backLink.setOnMouseClicked(e -> handleBackClick());
			}
		}

		public void setStage(Stage stage) {
			this.stage = stage;
		}

		public void setLoginController(ControllerLogin loginController) {
			this.loginController = loginController;
		}

		private void handleRegister() {
			String username = usernameField.getText().trim();
			String displayname = displaynameField.getText().trim();
			String password = passwordField.getText();

			if (validateInput(username, displayname, password)) {
				try {
					// Chat-View laden und Controller vorbereiten (für später)
					javafx.fxml.FXMLLoader chatLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/Client/chat-view.fxml"));
					javafx.scene.Parent chatRoot = chatLoader.load();

					chatController = chatLoader.getController();

					// Callback für Register-Response registrieren
					chatController.setOnRegisterResult(response -> {
						if (response.getStatus() == Status.SUCCESS) {
							// Registrierung erfolgreich - zurück zum Login
							showInfo("Registrierung erfolgreich! Bitte melden Sie sich an.");
							if (loginController != null) {
								loginController.goBackToLogin();
							}
						} else {
							showError("Registrierung fehlgeschlagen: " + (response.getMessage() != null ? response.getMessage() : "Unbekannter Fehler"));
						}
					});

					// Register-Request senden
					chatController.sendRegisterRequest(username, displayname, password);

				} catch (java.io.IOException ex) {
					showError("Fehler beim Laden der Chat-View: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}

		private void handleBackClick() {
			if (loginController != null) {
				loginController.goBackToLogin();
			}
		}

		private boolean validateInput(String username, String displayname, String password) {
			if (username.isEmpty()) {
				showError("Benutzername ist erforderlich");
				return false;
			}
			if (displayname.isEmpty()) {
				showError("Anzeigename ist erforderlich");
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

		private void showInfo(String message) {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Erfolg");
			alert.setHeaderText(null);
			alert.setContentText(message);
			alert.showAndWait();
		}
	}




