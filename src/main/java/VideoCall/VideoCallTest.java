package VideoCall;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class VideoCallTest extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		ImageView display = new ImageView();
		display.setFitWidth(640);
		display.setFitHeight(480);
		display.setPreserveRatio(true);

		StackPane root = new StackPane(display);
		Scene scene = new Scene(root, 640, 480);
		stage.setTitle("Video Call Test");
		stage.setScene(scene);
		stage.show();

		// Start sender and receiver on same PC for testing
		VideoSender sender = new VideoSender("127.0.0.1", 9001, "test-room");
		VideoReceiver receiver = new VideoReceiver(9001, display);

		sender.start();
		receiver.start();

		stage.setOnCloseRequest(e -> {
			sender.stop();
			receiver.stop();
		});
	}
}
