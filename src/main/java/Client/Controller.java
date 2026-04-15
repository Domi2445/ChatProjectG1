package Client;

import Util.ImageMessage;
import Util.Message;
import Util.TextMessage;
import Util.User;
import VideoCall.AudioCall;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Controller {
	public static final int MAX_FILE_SIZE = 1_000_000;

	private final BlockingQueue<Message> outgoingMessageQueue;
	private final BlockingQueue<Message> incomingMessageQueue;

	private View view;
	private Client client;
	private User localUser;


	private AudioCall audioCall = new AudioCall();
	private boolean inCall = false ;

	private void  handleCallButton()
	{
		if (!inCall)
		{
			String[] params = view.showCallDialog();
			if(params == null)
			{
				return;
			}

			try
			{
					audioCall.start(params[0] ,Integer.parseInt(params[1]), Integer.parseInt(params[2]));

					inCall = true;
					view.setCallActive(true);
			}
			catch(Exception ex)
			{
				view.getMessages().add(new TextMessage(new User("System"), "Anruf fehlgeschlagen: " + ex.getMessage()));

			}
		}
		else {
		audioCall.stop();
		audioCall = new AudioCall();
		inCall = false;
		view.setCallActive(false);
	}
	}




	public Controller() {
		this.outgoingMessageQueue = new ArrayBlockingQueue<>(4);
		this.incomingMessageQueue = new ArrayBlockingQueue<>(4);
	}

	public void initView(Stage stage, User user) {
		this.localUser = user;
		view = new View(stage, localUser);
		view.getSendButton().setOnAction(e -> sendMessage());
		view.getMessageTextField().setOnAction(e -> sendMessage());
		view.getUploadButton().setOnAction(e -> sendFile());
		view.getVideoCallButton().setOnAction(e -> handleCallButton());
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
			Message message = new TextMessage(localUser, text);

			try {
				outgoingMessageQueue.put(message);
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

			view.getMessageListView().scrollTo(view.getMessages().size() - 1);
			view.getMessageTextField().clear();
		}
	}

	private void sendFile() {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(view.getStage());

		if (selectedFile == null) { return; }

		if (selectedFile.length() > MAX_FILE_SIZE) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Die Datei ist zu groß!");
			alert.setContentText(selectedFile.length() + " Bytes / " + MAX_FILE_SIZE + " Bytes");
			alert.show();
			return;
		}

		byte[] bytes;

		try {
			bytes = Files.readAllBytes(selectedFile.toPath());
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Datei konnte nicht geöffnet werden");
			alert.setContentText(e.toString());
			alert.show();
			return;
		}

		Message message = new ImageMessage(localUser, bytes);

		try {
			outgoingMessageQueue.put(message);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		view.getMessageListView().scrollTo(view.getMessages().size() - 1);
		view.getMessageTextField().clear();
	}
}
