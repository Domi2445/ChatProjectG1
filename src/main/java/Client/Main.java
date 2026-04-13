package Client;

import Util.User;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		Controller controller = new Controller();
		controller.initView(primaryStage, new User("Benutzername"));
		controller.connectAndRun("127.0.0.1", 6969);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
