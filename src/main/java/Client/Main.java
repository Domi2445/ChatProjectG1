package Client;

import User.Model.User;
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
		// Login-Bildschirm laden
		FXMLLoader loginLoader = new FXMLLoader(Main.class.getResource("/Client/loginScreen.fxml"));
		Parent loginRoot = loginLoader.load();

		ControllerLogin loginController = loginLoader.getController();
		loginController.setStage(primaryStage);

		Scene scene = new Scene(loginRoot, 1280, 720);
		primaryStage.setTitle("Socket Chat - Login");
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(350);
		primaryStage.setMinHeight(400);
		primaryStage.show();
	}
}
