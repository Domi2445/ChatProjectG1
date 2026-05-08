package DBUtil;

import javafx.application.Application;
import javafx.application.Platform;
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
		FXMLLoader loader = new FXMLLoader(Main.class.getResource("/DBUtil/db-tabelle.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root, 430, 300);
		primaryStage.setTitle("DB Verwaltung");
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(380);
		primaryStage.setMinHeight(260);
		primaryStage.show();

		Thread titleLoader = new Thread(() -> {
			try {
				String databaseLabel = Connection.getActiveDatabaseLabel();
				Platform.runLater(() -> primaryStage.setTitle("DB Verwaltung - " + databaseLabel));
			} catch (RuntimeException e) {
				System.err.println("Datenbank konnte nicht beim Start initialisiert werden: " + e.getMessage());
				e.printStackTrace(System.err);
				Platform.runLater(() -> primaryStage.setTitle("DB Verwaltung - offline"));
			}
		}, "DatabaseLabelLoader");
		titleLoader.setDaemon(true);
		titleLoader.start();
	}
}

