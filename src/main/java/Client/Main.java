package Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

	public static final String CUPERTINO_DARK_CSS =
		Main.class.getResource("/Client/cupertino-dark.css").toExternalForm();

	public static void applyTheme(Dialog<?> dialog) {
		var sheets = dialog.getDialogPane().getStylesheets();
		if (!sheets.contains(CUPERTINO_DARK_CSS)) {
			sheets.add(CUPERTINO_DARK_CSS);
		}
	}

	public static <D extends Dialog<?>> D themed(D dialog) {
		applyTheme(dialog);
		return dialog;
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		FXMLLoader chatLoader = new FXMLLoader(Main.class.getResource("/Client/chatScreen.fxml"));
		Parent chatRoot = chatLoader.load();
		Controller chatController = chatLoader.getController();
		chatController.configure(primaryStage, null);
		String host = System.getProperty("server.host",
			System.getenv().getOrDefault("SERVER_HOST", "192.168.101.114"));
		int port = Integer.parseInt(System.getProperty("server.port",
			System.getenv().getOrDefault("SERVER_PORT", "3299")));
		if (!chatController.connectAndRun(host, port)) {
		if (!chatController.connectAndRun("127.0.0.1", 3299)) {
			return;
		}
			// Audio - Beim Schließen des Fensters den Anruf beenden
			primaryStage.setOnCloseRequest(e -> {
				chatController.stopCall();
				Platform.exit();
				System.exit(0);
			});


		Scene chatScene = new Scene(chatRoot, 1280, 720);
		chatScene.getStylesheets().add(CUPERTINO_DARK_CSS);

		FXMLLoader loginLoader = new FXMLLoader(Main.class.getResource("/Client/loginScreen.fxml"));
		Parent loginRoot = loginLoader.load();
		ControllerLogin loginController = loginLoader.getController();
		loginController.setStage(primaryStage);
		loginController.setController(chatController);
		loginController.setChatScene(chatScene);

		Scene loginScene = new Scene(loginRoot, 1280, 720);
		loginScene.getStylesheets().add(CUPERTINO_DARK_CSS);

		primaryStage.setTitle("Socket Chat - Login");
		primaryStage.setScene(loginScene);
		primaryStage.setMinWidth(350);
		primaryStage.setMinHeight(400);
		primaryStage.show();


	}
}
