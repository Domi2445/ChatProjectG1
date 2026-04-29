package Client;

import User.Login.Status;
import User.Model.User;
import Util.FileUtil;
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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

//Audio
import AudioCall.AudioCall;
import Util.Network.Notifications.CallNotification;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.scene.control.TextInputDialog;

public class Controller {
	public static final int MAX_FILE_SIZE = 1_000_000;

	private final BlockingQueue<Packet> outPacketQueue;
	private final BlockingQueue<Packet> inPacketQueue;

	private Consumer<LoginResponse> onLoginResult;
	private Consumer<RegisterResponse> onRegisterResult;

	private Client client;
	private User localUser;
	private Stage stage;
	private TextMessage isEditingMessage;

	// Audio Call
	private static final String RELAY_IP = "127.0.0.1";
	private static final int RELAY_PORT = 8000;
	private final AudioCall audioCall = new AudioCall();
	private boolean inCall = false;

	@FXML
	private ListView<Packet> messageListView;

	@FXML
	private TextField messageTextField;

	@FXML
	private Button sendButton;

	@FXML
	private Button uploadButton;
	//Audio
	@FXML
	private Button videoCallButton;

	public Controller() {
		this.outPacketQueue = new ArrayBlockingQueue<>(4);
		this.inPacketQueue = new ArrayBlockingQueue<>(4);
	}
	
	// UI registriert hier ihren Handler (z.B. Screen-Wechsel bei Success)
	public void setOnLoginResult(Consumer<LoginResponse> onLoginResult) {
		this.onLoginResult = onLoginResult;
	}

	public void setOnRegisterResult(Consumer<RegisterResponse> onRegisterResult) {
		this.onRegisterResult = onRegisterResult;
	}

	@FXML
	private void initialize() {
		messageListView.setCellFactory(lv -> new MessageCell());
		
		sendButton.setOnAction(e -> sendMessage());
		messageTextField.setOnAction(e -> sendMessage());
		uploadButton.setOnAction(e -> sendFile());
		//Audio
		videoCallButton.setOnAction(e -> handleCallButton());
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

							//Audio
							case CallNotification call -> Platform.runLater(() -> handleCallNotification(call));

							case Notification notification -> Platform.runLater(() -> {
								getMessages().add(notification);
								messageListView.scrollTo(getMessages().size() - 1);
								handleNotification(notification);
							});
							case LoginResponse loginResp -> Platform.runLater(() -> { //FÜR UI CALLBACK
								handleLoginResponse(loginResp);
							});
							case RegisterResponse registerResp -> Platform.runLater(() -> {
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
			Alert alert = new Alert(Alert.AlertType.ERROR, e.getLocalizedMessage() + "\n\nErneut verbinden?", ButtonType.YES, ButtonType.NO);
			alert.setHeaderText("Verbindung fehlgeschlagen");
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					connectAndRun(ip, port);
				}
			});
		}
	}

	private ObservableList<Packet> getMessages() {
		return messageListView.getItems();
	}

	private void sendMessage() {
		String text = messageTextField.getText().trim();
		if (!text.isEmpty()) {
			if (isEditingMessage != null) {
				isEditingMessage.setEditedContent(text);
				int index = getMessages().indexOf(isEditingMessage);
				if (index >= 0) {
					messageListView.getItems().set(index, isEditingMessage);
					messageListView.refresh();
				}
				isEditingMessage = null;
				resetSendButton();
			} else {
				Message message = new TextMessage(localUser, text);
				try {
					outPacketQueue.put(message);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}

			messageListView.scrollTo(getMessages().size() - 1);
			messageTextField.clear();
		}
	}
	
	private void resetSendButton() {
		sendButton.setText("Send");
		sendButton.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 70; -fx-min-height: 40;");
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

		String fileName = selectedFile.getName();
		String fileExt = FileUtil.getFileExtension(fileName).toLowerCase();
		Message message = new FileMessage(localUser, bytes, fileExt);

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
				System.out.println(join.getUser().getUsername() + " ist beigetreten");
				// todo(team-view): in der View einen neuen Nutzer anzeigen (z. B. In Seitenleiste oder direkt im Chat)
			}
			case LeaveNotification leave -> {
				System.out.println(leave.getUser().getUsername() + " hat verlassen");
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
		if (response.getStatus() == Status.SUCCESS) {
			this.localUser = response.getUser();
		}
		if (onLoginResult != null) {
			onLoginResult.accept(response);
		}
	}

	private void handleRegisterResponse(RegisterResponse response) {
		if (response.getStatus() == Status.SUCCESS) {
			this.localUser = response.getUser();
		}

		if (onRegisterResult != null) {
			onRegisterResult.accept(response);
		}
	}

	private class MessageCell extends ListCell<Packet> {
		private TextMessage editingMessage;

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

				//Audio
				//default -> throw new IllegalStateException("Unbekannte Servernachricht: " + item);
				case null, default -> {}
			}
		}

		private HBox renderMessageBubble(Message message) {
			Node node;

			switch (message) {
				case TextMessage textMessage -> {
					String text = textMessage.isDeleted()
						? "Diese Nachricht wurde gelöscht"
						: textMessage.getContent();

					Label label = new Label(text);
					label.setWrapText(true);
					label.setMaxWidth(300);

					if (textMessage.isDeleted()) {
						label.setStyle("-fx-text-fill: #6c7086; -fx-font-style: italic;");
					}

					node = label;
				}
				case FileMessage fileMessage -> node = createFileNode(fileMessage);
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + message);
			}

			boolean isOwn = localUser != null && localUser.equals(message.getSender());
			node.setStyle(getBubbleStyle(isOwn));

			VBox messageBox = new VBox(2);
			messageBox.getChildren().add(node);

			if (message instanceof TextMessage textMessage && textMessage.isEdited() && !textMessage.isDeleted()) {
				Label editedLabel = new Label("bearbeitet");
				editedLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #6c7086; -fx-font-style: italic;");
				messageBox.getChildren().add(editedLabel);
			}

			HBox container = new HBox(messageBox);
			container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
			container.setPadding(new Insets(2, 10, 2, 10));

			if (isOwn && message instanceof TextMessage textMessage && !textMessage.isDeleted()) {
				container.setOnContextMenuRequested(event -> {
					ContextMenu contextMenu = createMessageContextMenu(textMessage);
					contextMenu.show(container, event.getScreenX(), event.getScreenY());
				});
			}

			return container;
		}

		private Node createFileNode(FileMessage fileMessage) {
			if (FileUtil.isImageExtension(fileMessage.getFileExtension())) {
				Image image = new Image(new ByteArrayInputStream(fileMessage.getContent()));
				ImageView imageView = new ImageView(image);
				imageView.setPreserveRatio(true);
				imageView.fitWidthProperty().bind(Bindings.createDoubleBinding(
					() -> Math.clamp(getScene().getWidth() - 32, 100.0, image.getWidth()),
					getScene().widthProperty()
				));
				return imageView;
			} else {
				Label label = new Label(fileMessage.getFileExtension() + "-Datei");
				// todo(team-view): Datei herunterladen Button
				return label;
			}
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
					text = join.getUser().getUsername() + " ist beigetreten";
					color = "#89b4fa";
				}
				case LeaveNotification leave -> {
					text = leave.getUser().getUsername() + " hat verlassen";
					color = "#f38ba8";
				}
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + notification);
			}

			setText(text);
			setAlignment(Pos.CENTER);
			setStyle("-fx-background-color: transparent; -fx-padding: 4 0; "
				+ "-fx-text-fill: " + color + "; -fx-font-style: italic;");
		}

		private ContextMenu createMessageContextMenu(TextMessage message) {
			ContextMenu menu = new ContextMenu();
			
			MenuItem editItem = new MenuItem("✏️ Bearbeiten");
			editItem.setStyle("-fx-font-size: 12;");
			editItem.setOnAction(event -> Controller.this.startEditMessage(message));
			
			MenuItem deleteItem = new MenuItem("🗑️ Löschen");
			deleteItem.setStyle("-fx-font-size: 12;");
			deleteItem.setOnAction(event -> Controller.this.deleteMessage(message));
			
			menu.getItems().addAll(editItem, deleteItem);
			return menu;
		}
	}



	// Wird aufgerufen wenn der Nutzer auf den Anruf-Button drückt
	public void handleCallButton() {
		if (!inCall) {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Anruf starten");
			dialog.setHeaderText("Benutzername des Empfängers:");
			String target = dialog.showAndWait().orElse(null);
			if (target == null || target.isBlank()) return;
			try {
				outPacketQueue.put(new CallNotification(
					CallNotification.CallType.REQUEST, localUser, target, 0));
			} catch (InterruptedException e) { e.printStackTrace(); }
		} else {
			audioCall.stop();
			inCall = false;
		}
	}
//Audio
	private void handleCallNotification(CallNotification call) {

		if (localUser == null || !call.getTargetUsername().equals(localUser.getUsername())) return;

		switch (call.getType()) {
			case REQUEST -> {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Eingehender Anruf");
				alert.setHeaderText("Anruf von: " + call.getSender().getUsername());
				boolean accepted = alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
				try {
					outPacketQueue.put(new CallNotification(
						accepted ? CallNotification.CallType.ACCEPT : CallNotification.CallType.REJECT,
						localUser, call.getSender().getUsername(), 0));
					if (accepted) startAudioCall(call);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
			case ACCEPT -> startAudioCall(call);
			case REJECT -> getMessages().add(
				new TextMessage(localUser, call.getSender().getUsername() + " hat abgelehnt."));
		}
	}

	private void startAudioCall(CallNotification call) {
		String roomId = Stream.of(localUser.getUsername(), call.getSender().getUsername())
			.sorted().collect(Collectors.joining("-"));
		try {
			audioCall.start(RELAY_IP, RELAY_PORT, roomId);
			inCall = true;
		} catch (Exception e) { e.printStackTrace(); }
	}






	
	private void startEditMessage(TextMessage message) {
		messageTextField.setText(message.getContent());
		messageTextField.requestFocus();
		isEditingMessage = message;
		sendButton.setText("Speichern");
		sendButton.setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 70; -fx-min-height: 40;");
	}

	private void deleteMessage(TextMessage message) {
		message.setDeleted();
		int index = getMessages().indexOf(message);
		if (index >= 0) {
			messageListView.getItems().set(index, message);
		}
	}
}
