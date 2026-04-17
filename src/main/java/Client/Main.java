package Client;

import Util.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		FXMLLoader loader = new FXMLLoader(Main.class.getResource("/Client/chat-view.fxml"));
		Parent root = loader.load();

		Controller controller = loader.getController();
		String defaultUsername = "Benutzer-" + System.currentTimeMillis();
		controller.configure(primaryStage, new User(defaultUsername));

		Scene scene = new Scene(root, 500, 650);
		primaryStage.setTitle("Socket Chat");
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(350);
		primaryStage.setMinHeight(400);
		primaryStage.show();

		controller.connectAndRun("127.0.0.1", 6969);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
