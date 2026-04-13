package Client;

import Util.Message;
import Util.TextMessage;
import Util.User;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Controller {
	private final BlockingQueue<Message> outgoingMessageQueue;
	private final BlockingQueue<Message> incomingMessageQueue;

	private View view;
	private Client client;

	public Controller() {
		this.outgoingMessageQueue = new ArrayBlockingQueue<>(4);
		this.incomingMessageQueue = new ArrayBlockingQueue<>(4);
	}

	public void initView(Stage stage) {
		view = new View(stage);
		view.getSendButton().setOnAction(e -> sendMessage());
		view.getMessageTextField().setOnAction(e -> sendMessage());
		view.getUploadButton().setOnAction(e -> sendFile());
	}

	public void connectAndRun(String ip, int port) {
		try {
			client = new Client(ip, port, outgoingMessageQueue, incomingMessageQueue);
			Thread clientThread = new Thread(client, "ClientThread");
			clientThread.setDaemon(true);
			clientThread.start();

			Thread listener = new Thread(() -> {
				while (true) {
					try {
						Message message = incomingMessageQueue.take();
						Platform.runLater(() -> {
							view.getMessages().add(message);
							view.getMessageListView().scrollTo(view.getMessages().size() - 1);
						});
					} catch (InterruptedException e) {
						break;
					}
				}
			}, "IncomingMessageListener");
			listener.setDaemon(true);
			listener.start();

		} catch (IOException e) {
			Platform.runLater(() ->
					view.getMessages().add(new TextMessage(new User("System"), "Verbindung fehlgeschlagen: " + e.getMessage()))
			);
		}
	}

	private void sendMessage() {
		String text = view.getMessageTextField().getText().trim();
		if (!text.isEmpty()) {
			try {
				Message message = new TextMessage(new User("Du"), text);
				outgoingMessageQueue.put(message);
				view.getMessageListView().scrollTo(view.getMessages().size() - 1);
				view.getMessageTextField().clear();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	private void sendFile() {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(view.getStage());

		if (selectedFile != null) {
			System.out.println(selectedFile.getName());

			byte[] bytes;

			try {
				bytes = Files.readAllBytes(selectedFile.toPath());
			} catch (IOException e) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setHeaderText("Datei konnte nicht geöffnet werden");
				alert.setContentText(e.toString());
				alert.show();
			}

			// todo
		}
	}
}
