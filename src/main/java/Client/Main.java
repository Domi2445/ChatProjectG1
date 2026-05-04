package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		FXMLLoader chatLoader = new FXMLLoader(Main.class.getResource("/Client/chat-view.fxml"));
		Parent chatRoot = chatLoader.load();
		Controller chatController = chatLoader.getController();
		chatController.configure(primaryStage, null);
		String host = System.getProperty("server.host",
			System.getenv().getOrDefault("SERVER_HOST", "217.154.156.40"));
		int port = Integer.parseInt(System.getProperty("server.port",
			System.getenv().getOrDefault("SERVER_PORT", "443")));
		if (!chatController.connectAndRun(host, port)) {
			return;
		}

		Scene chatScene = new Scene(chatRoot, 1280, 720);

		FXMLLoader loginLoader = new FXMLLoader(Main.class.getResource("/Client/loginScreen.fxml"));
		Parent loginRoot = loginLoader.load();
		ControllerLogin loginController = loginLoader.getController();
		loginController.setStage(primaryStage);
		loginController.setController(chatController);
		loginController.setChatScene(chatScene);

		primaryStage.setTitle("Socket Chat - Login");
		primaryStage.setScene(new Scene(loginRoot, 1280, 720));
		primaryStage.setMinWidth(350);
		primaryStage.setMinHeight(400);
		primaryStage.show();
	}
}
