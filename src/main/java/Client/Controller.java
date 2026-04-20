package Client;

import User.Login.Status;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.LoginResponse;
import Util.Network.Auth.RegisterRequest;
import Util.Network.Auth.RegisterResponse;
import Util.Network.Messages.FileMessage;
import Util.Network.Messages.Message;
import Util.Network.Messages.TextMessage;
import Util.Network.Notifications.JoinNotification;
import Util.Network.Notifications.LeaveNotification;
import Util.Network.Notifications.Notification;
import Util.Network.Packet;
import User.Model.User;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class Controller {
	public static final int MAX_FILE_SIZE = 1_000_000;

	private final BlockingQueue<Packet> outPacketQueue;
	private final BlockingQueue<Packet> inPacketQueue;



	private Consumer<LoginResponse> onLoginResult;
	private Consumer<RegisterResponse> onRegisterResult;

	// UI registriert hier ihren Handler (z.B. Screen-Wechsel bei Success)
	public void setOnLoginResult(Consumer<LoginResponse> onLoginResult) {
		this.onLoginResult = onLoginResult;
	}
	public void setOnRegisterResult(Consumer<RegisterResponse> onRegisterResult) {
		this.onRegisterResult = onRegisterResult;
	}

	private Client client;
	private User localUser;
	private Stage stage;

	@FXML
	private ListView<Packet> messageListView;

	@FXML
	private TextField messageTextField;

	@FXML
	private Button sendButton;

	@FXML
	private Button uploadButton;

	public Controller() {
		this.outPacketQueue = new ArrayBlockingQueue<>(4);
		this.inPacketQueue = new ArrayBlockingQueue<>(4);
	}

	@FXML
	private void initialize() {
		messageListView.setCellFactory(lv -> new MessageCell());
		sendButton.setOnAction(e -> sendMessage());
		messageTextField.setOnAction(e -> sendMessage());
		uploadButton.setOnAction(e -> sendFile());
	}

	public void configure(Stage stage, User user) {
		this.stage = stage;
		this.localUser = user;
	}

	public void connectAndRun(String ip, int port) {
		try {
			client = new Client(ip, port, outPacketQueue, inPacketQueue);
			Thread clientThread = new Thread(client, "ClientThread");
			clientThread.setDaemon(true);
			clientThread.start();

			Thread listener = new Thread(() -> {
				while (true) {
					try {
						Packet packet = inPacketQueue.take();
						switch (packet) {
							case Message message -> Platform.runLater(() -> {
								getMessages().add(message);
								messageListView.scrollTo(getMessages().size() - 1);
							});
							case Notification notification -> Platform.runLater(() -> {
								getMessages().add(notification);
								messageListView.scrollTo(getMessages().size() - 1);
								handleNotification(notification);
							});
							case LoginResponse loginResp -> Platform.runLater(() -> { //FÜR UI CALLBACK
								handleLoginResponse(loginResp);
							});
							case RegisterResponse registerResp -> Platform.runLater(()->{
								handleRegisterResponse(registerResp);
							});
							case null, default -> throw new IllegalStateException("Unbekanntes Paket empfangen");
						}


					} catch (InterruptedException e) {
						break;
					}
				}
			}, "IncomingMessageListener");
			listener.setDaemon(true);
			listener.start();

		} catch (IOException e) {
			// todo(team-view): schöner Fehler anzeigen (z. B. Popup) und Möglichkeit zum erneuten Verbinden anbieten
			User user = new User();
			user.setUsername("System");

			Platform.runLater(() ->
				getMessages().add(new TextMessage(user, "Verbindung fehlgeschlagen: " + e.getMessage()))
			);
		}
	}

	private ObservableList<Packet> getMessages() {
		return messageListView.getItems();
	}

	private void sendMessage() {
		String text = messageTextField.getText().trim();
		if (!text.isEmpty()) {
			Message message = new TextMessage(localUser, text);

			try {
				outPacketQueue.put(message);
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

			messageListView.scrollTo(getMessages().size() - 1);
			messageTextField.clear();
		}
	}

	private void sendFile() {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(stage);

		if (selectedFile == null || !selectedFile.isFile()) {
			return;
		}

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

		FileMessage.FileType fileType;
		String fileName = selectedFile.getName();

		if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".gif") || fileName.endsWith(".bmp")) {
			fileType = FileMessage.FileType.IMAGE;
		} else {
			fileType = FileMessage.FileType.FILE;
		}

		Message message = new FileMessage(localUser, bytes, fileName, fileType);

		try {
			outPacketQueue.put(message);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		messageListView.scrollTo(getMessages().size() - 1);
		messageTextField.clear();
	}

	private void handleNotification(Notification notification) {
		switch (notification) {
			case JoinNotification join -> {
				System.out.println(join.getUser() + " ist beigetreten");
				// todo(team-view): in der View einen neuen Nutzer anzeigen (z. B. In Seitenleiste oder direkt im Chat)
			}
			case LeaveNotification leave -> {
				System.out.println(leave.getUser() + " hat verlassen");
				// todo(team-view): in der View den angezeigten Benutzer entfernen
			}
			case null, default -> throw new IllegalStateException("Unbekannte Systemnachricht");
		}

	}

	public void sendLoginRequest(String username, String password) {
		LoginRequest request = new LoginRequest(username, password);
		try {
			outPacketQueue.put(request);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	public void sendRegisterRequest(String username, String displayname, String password) {
		RegisterRequest request = new RegisterRequest(username, displayname, password);
		try {
			outPacketQueue.put(request);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	private void handleLoginResponse(LoginResponse response) {
		if(response.getStatus() == Status.SUCCESS){
			this.localUser = response.getUser();
		} if (onLoginResult != null) {
			onLoginResult.accept(response);
		}
	}
	private void handleRegisterResponse(RegisterResponse response){
		if(onRegisterResult != null){
			onRegisterResult.accept(response);
		}
	}

	private class MessageCell extends ListCell<Packet> {
		@Override
		protected void updateItem(Packet item, boolean empty) {
			super.updateItem(item, empty);

			setText(null);
			setGraphic(null);
			setStyle("-fx-background-color: transparent; -fx-padding: 0;");

			if (empty || item == null) {
				return;
			}

			switch (item) {
				case Message message -> setGraphic(renderMessageBubble(message));
				case Notification notification -> renderNotificationLine(notification);
				default -> throw new IllegalStateException("Unbekannte Servernachricht: " + item);
			}
		}

		private HBox renderMessageBubble(Message message) {
			Node node;

			switch (message) {
				case TextMessage textMessage -> {
					Label label = new Label(textMessage.getContent());
					label.setWrapText(true);
					label.setMaxWidth(300);
					node = label;
				}
				case FileMessage fileMessage -> node = createFileNode(fileMessage);
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + message);
			}

			boolean isOwn = localUser != null && localUser.equals(message.getSender());
			node.setStyle(getBubbleStyle(isOwn));

			HBox container = new HBox(node);
			container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
			container.setPadding(new Insets(2, 10, 2, 10));
			return container;
		}

		private Node createFileNode(FileMessage fileMessage) {
			return switch (fileMessage.getFileType()) {
				case FILE -> {
					Label label = new Label("Datei: " + fileMessage.getFileName());
					// todo(team-view): Datei herunterladen Button
					yield label;
				}
				case IMAGE -> {
					Image image = new Image(new ByteArrayInputStream(fileMessage.getContent()));
					ImageView imageView = new ImageView(image);
					imageView.setPreserveRatio(true);
					imageView.fitWidthProperty().bind(Bindings.createDoubleBinding(
						() -> Math.clamp(getScene().getWidth() - 32, 100.0, image.getWidth()),
						getScene().widthProperty()
					));
					yield imageView;
				}
			};
		}

		private String getBubbleStyle(boolean isOwn) {
			if (isOwn) {
				return "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; "
					+ "-fx-padding: 8 12; -fx-background-radius: 14 14 4 14;";
			}
			return "-fx-background-color: #313244; -fx-text-fill: #cdd6f4; "
				+ "-fx-padding: 8 12; -fx-background-radius: 14 14 14 4;";
		}

		private void renderNotificationLine(Notification notification) {
			String text;
			String color;

			switch (notification) {
				case JoinNotification join -> {
					text = join.getUser() + " ist beigetreten";
					color = "#89b4fa";
				}
				case LeaveNotification leave -> {
					text = leave.getUser() + " hat verlassen";
					color = "#f38ba8";
				}
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + notification);
			}

			setText(text);
			setAlignment(Pos.CENTER);
			setStyle("-fx-background-color: transparent; -fx-padding: 4 0; "
				+ "-fx-text-fill: " + color + "; -fx-font-style: italic;");
		}
	}
}
