package DBUtil;

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
		FXMLLoader loader = new FXMLLoader(Main.class.getResource("/DBUtil/db-Tabelle.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root, 430, 300);
		primaryStage.setTitle("DB Verwaltung");
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(380);
		primaryStage.setMinHeight(260);
		primaryStage.show();
	}
}

