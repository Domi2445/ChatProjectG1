package Client;

import AudioCall.AudioCall;
import User.Login.Status;
import User.Model.User;
import User.Model.ChatMessage;
import User.Model.MessageType;
import Util.FileUtil;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.LoginResponse;
import Util.Network.Auth.RegisterRequest;
import Util.Network.Auth.RegisterResponse;
import Util.Network.ConnectionClosed;
import Util.Network.DeleteMessage;
import Util.Network.HistoryRequest;
import Util.Network.HistoryResponse;
import Util.Network.Messages.FileMessage;
import Util.Network.Messages.Message;
import Util.Network.Messages.TextMessage;
import Util.Network.Notifications.JoinNotification;
import Util.Network.Notifications.LeaveNotification;
import Util.Network.Notifications.CallNotification;
import Util.Network.Notifications.Notification;
import Util.Network.Packet;
import Util.Network.ProfilePictureUpdate;
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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {
	public static final int MAX_FILE_SIZE = 1_000_000;
	public static final int MAX_PROFILE_PICTURE_SIZE = 500_000;
	public static final int PROFILE_PICTURE_SIZE = 96;
	public static final int PACKET_QUEUE_SIZE = 128;
	private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private final BlockingQueue<Packet> outPacketQueue;
	private final BlockingQueue<Packet> inPacketQueue;

	private Consumer<LoginResponse> onLoginResult;
	private Consumer<RegisterResponse> onRegisterResult;

	private Client client;
	private User localUser;
	private Stage stage;
	private TextMessage isEditingMessage;
	private final Map<String, byte[]> profilePicturesByUsername = new HashMap<>();
	private final Map<String, String> profilePictureContentTypesByUsername = new HashMap<>();
	private boolean profilePictureSyncEnabled;
	private static final String RELAY_IP = "217.154.156.40";
	private static final int RELAY_PORT = 3268;
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

	@FXML
	private Button videoCallButton;

	@FXML
	private Button profilePictureButton;

	@FXML
	private ImageView profilePictureView;

	public Controller() {
		this.outPacketQueue = new ArrayBlockingQueue<>(PACKET_QUEUE_SIZE);
		this.inPacketQueue = new ArrayBlockingQueue<>(PACKET_QUEUE_SIZE);
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
		profilePictureButton.setOnAction(e -> uploadProfilePicture());
		videoCallButton.setOnAction(e -> handleCallButton());
	}

	public void configure(Stage stage, User user) {
		this.stage = stage;
		this.localUser = user;
	}

	public boolean connectAndRun(String ip, int port) {
		try {
			profilePictureSyncEnabled = Boolean.parseBoolean(System.getProperty("profile.sync", "true"));
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
								// Sende ReadReceipt, wenn die Nachricht nicht von uns selbst stammt
								if (localUser != null && message.getSender() != null && !message.getSender().equals(localUser)) {
									try {
										Util.Network.ReadReceipt receipt = new Util.Network.ReadReceipt(message.getMessageId(), localUser.getUsername());
										outPacketQueue.put(receipt);
									} catch (InterruptedException e) {
										throw new RuntimeException(e);
									}
								}
								messageListView.scrollTo(getMessages().size() - 1);
							});
							case CallNotification call -> Platform.runLater(() -> handleCallNotification(call));
							case Notification notification -> Platform.runLater(() -> {
								getMessages().add(notification);
								messageListView.scrollTo(getMessages().size() - 1);
								handleNotification(notification);
							});
							case Util.Network.ReadReceipt receipt -> Platform.runLater(() -> {
								for (Packet p : getMessages()) {
									if (p instanceof Message msg && msg.getMessageId() == receipt.getMessageId()) {
										msg.markAsReadBy(receipt.getUsername());
										messageListView.refresh();
										break;
									}
								}
							});
							case Util.Network.EditMessage edit -> Platform.runLater(() -> {
								for (int i = 0; i < getMessages().size(); i++) {
									Packet p = getMessages().get(i);
									if (p instanceof TextMessage msg && msg.getMessageId() == edit.getMessageId()) {
										msg.setEditedContent(edit.getNewContent());
										messageListView.getItems().set(i, msg);
										messageListView.refresh();
										break;
									}
								}
							});
							case Util.Network.DeleteMessage delete -> Platform.runLater(() -> {
								for (int i = 0; i < getMessages().size(); i++) {
									Packet p = getMessages().get(i);
									if (p instanceof TextMessage msg && msg.getMessageId() == delete.getMessageId()) {
										msg.setDeleted();
										messageListView.getItems().set(i, msg);
										messageListView.refresh();
										break;
									} else if (p instanceof FileMessage msg && msg.getMessageId() == delete.getMessageId()) {
										msg.setDeleted();
										messageListView.getItems().set(i, msg);
										messageListView.refresh();
										break;
									}
								}
							});
							case ProfilePictureUpdate update -> Platform.runLater(() -> applyProfilePictureUpdate(update));
							case ConnectionClosed closed -> Platform.runLater(() -> handleConnectionClosed(closed));
							case LoginResponse loginResp -> Platform.runLater(() -> { //FÜR UI CALLBACK
								handleLoginResponse(loginResp);
							});
							case RegisterResponse registerResp -> Platform.runLater(() -> {
								handleRegisterResponse(registerResp);
							});
							case HistoryResponse histResp -> Platform.runLater(() -> {
								// Laden die Chat-History vom Server
								if ("success".equals(histResp.getStatus()) && histResp.getMessages() != null) {
									getMessages().clear();
									// Konvertiere ChatMessage in Message/TextMessage für die UI
									for (ChatMessage dbMsg : histResp.getMessages()) {
										if (dbMsg.getMessageType() == MessageType.TEXT || dbMsg.getMessageType() == MessageType.EMOJI) {
											Message msg = new TextMessage(new User(dbMsg.getSender()), dbMsg.getContent());
											getMessages().add(msg);
										} else if (dbMsg.getMessageType() == MessageType.FILE) {
											Message msg = new TextMessage(new User(dbMsg.getSender()), "[Datei: " + dbMsg.getFilePath() + "]");
											getMessages().add(msg);
										}
									}
									messageListView.refresh();
									System.out.println("Chat-History geladen: " + histResp.getMessages().size() + " Messages");
								} else {
									System.err.println("Fehler beim Laden der History: " + histResp.getErrorMessage());
								}
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
			return true;

		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR, e.getLocalizedMessage() + "\n\nErneut verbinden?", ButtonType.YES, ButtonType.NO);

			alert.setHeaderText("Verbindung fehlgeschlagen");

			var response = alert.showAndWait();
			if (response.isPresent() && response.get() == ButtonType.YES) {
				return connectAndRun(ip, port);
			}

			return false;
		}
	}

	private ObservableList<Packet> getMessages() {
		return messageListView.getItems();
	}

	private boolean sendPacket(Packet packet) {
		if (!outPacketQueue.offer(packet)) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Nachricht konnte nicht gesendet werden");
			alert.setContentText("Die Verbindung ist gerade ausgelastet. Bitte versuche es gleich nochmal.");
			alert.show();
			return false;
		}
		return true;
	}

	private void sendMessage() {
		String text = messageTextField.getText().trim();
		if (!text.isEmpty()) {
			if (isEditingMessage != null) {
				if (!sendPacket(new Util.Network.EditMessage(isEditingMessage.getMessageId(), text))) {
					return;
				}
				isEditingMessage = null;
				resetSendButton();
			} else {
				Message message = new TextMessage(createNetworkUser(localUser), text);
				if (!sendPacket(message)) {
					return;
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
		Message message = new FileMessage(createNetworkUser(localUser), bytes, fileExt);

		if (!sendPacket(message)) {
			return;
		}

		messageListView.scrollTo(getMessages().size() - 1);
		messageTextField.clear();
	}

	private void uploadProfilePicture() {
		if (localUser == null) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Profilbild auswaehlen");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg", "*.gif"));
		File selectedFile = fileChooser.showOpenDialog(stage);

		if (selectedFile == null || !selectedFile.isFile()) {
			return;
		}

		if (selectedFile.length() > MAX_FILE_SIZE) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Das Profilbild ist zu gross!");
			alert.setContentText(selectedFile.length() + " Bytes / " + MAX_FILE_SIZE + " Bytes");
			alert.show();
			return;
		}

		String extension = FileUtil.getFileExtension(selectedFile.getName()).toLowerCase();
		if (!FileUtil.isImageExtension(extension)) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Bitte eine Bilddatei auswaehlen");
			alert.show();
			return;
		}

		try {
			byte[] bytes = createProfilePictureBytes(selectedFile);
			if (bytes.length > MAX_PROFILE_PICTURE_SIZE) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setHeaderText("Profilbild konnte nicht verkleinert werden");
				alert.setContentText(bytes.length + " Bytes / " + MAX_PROFILE_PICTURE_SIZE + " Bytes");
				alert.show();
				return;
			}
			localUser.setProfilePicture(bytes);
			localUser.setProfilePictureContentType("image/jpeg");
			cacheProfilePicture(localUser.getUsername(), bytes, localUser.getProfilePictureContentType());
			updateOwnProfilePictureView();
			messageListView.refresh();
			if (profilePictureSyncEnabled) {
				sendPacket(new ProfilePictureUpdate(localUser.getUsername(), bytes, "image/jpeg"));
			}
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Profilbild konnte nicht geoeffnet werden");
			alert.setContentText(e.toString());
			alert.show();
		}
	}

	private String normalizeImageExtension(String extension) {
		return "jpg".equals(extension) ? "jpeg" : extension;
	}

	private byte[] createProfilePictureBytes(File selectedFile) throws IOException {
		BufferedImage original = ImageIO.read(selectedFile);
		if (original == null) {
			throw new IOException("Bildformat konnte nicht gelesen werden");
		}

		BufferedImage scaled = new BufferedImage(PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = scaled.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(java.awt.Color.WHITE);
			graphics.fillRect(0, 0, PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE);

			int sourceSize = Math.min(original.getWidth(), original.getHeight());
			int sourceX = (original.getWidth() - sourceSize) / 2;
			int sourceY = (original.getHeight() - sourceSize) / 2;
			graphics.drawImage(original, 0, 0, PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE,
				sourceX, sourceY, sourceX + sourceSize, sourceY + sourceSize, null);
		} finally {
			graphics.dispose();
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (!ImageIO.write(scaled, "jpg", output)) {
			throw new IOException("Profilbild konnte nicht als JPG gespeichert werden");
		}
		return output.toByteArray();
	}

	private void handleNotification(Notification notification) {
		switch (notification) {
			case JoinNotification join -> {
				System.out.println(join.getUser().getUsername() + " ist beigetreten");
				cacheProfilePicture(join.getUser());
				// todo(team-view): in der View einen neuen Nutzer anzeigen (z. B. In Seitenleiste oder direkt im Chat)
			}
			case LeaveNotification leave -> {
				System.out.println(leave.getUser().getUsername() + " hat verlassen");
				// todo(team-view): in der View den angezeigten Benutzer entfernen
			}
			case null, default -> throw new IllegalStateException("Unbekannte Systemnachricht");
		}

	}

	private void handleConnectionClosed(ConnectionClosed closed) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("Verbindung zum Server getrennt");
		alert.setContentText(closed.getReason());
		alert.show();
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

			cacheProfilePicture(localUser);
			updateOwnProfilePictureView();
			// Nach erfolgreichem Login: lade Chat-History vom Server
			try {
				HistoryRequest histReq = new HistoryRequest();
				histReq.setSender(localUser.getUsername());  // Sender = aktueller Benutzer
				histReq.setReceiver(null);                   // Global chat
				histReq.setChatRoomId("main");               // Standard chat room für globale Nachrichten
				outPacketQueue.put(histReq);
			} catch (InterruptedException e) {
				System.err.println("Fehler beim Senden von HistoryRequest: " + e.getMessage());
			}
		}
		if (onLoginResult != null) {
			onLoginResult.accept(response);
		}
	}

	private void handleRegisterResponse(RegisterResponse response) {
		if (response.getStatus() == Status.SUCCESS) {
			this.localUser = response.getUser();
			cacheProfilePicture(localUser);
			updateOwnProfilePictureView();
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
				default -> throw new IllegalStateException("Unbekannte Servernachricht: " + item);
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
				case FileMessage fileMessage -> {
					if (fileMessage.isDeleted()) {
						Label label = new Label("Diese Datei wurde geloescht");
						label.setWrapText(true);
						label.setMaxWidth(300);
						label.setStyle("-fx-text-fill: #6c7086; -fx-font-style: italic;");
						node = label;
					} else {
						node = createFileNode(fileMessage);
					}
				}
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + message);
			}

			boolean isOwn = localUser != null
				&& message.getSender() != null
				&& localUser.getUsername() != null
				&& localUser.getUsername().equals(message.getSender().getUsername());
			node.setStyle(getBubbleStyle(isOwn));

			VBox messageBox = new VBox(2);
			Label metaLabel = new Label(getDisplayName(message.getSender()) + "  " + getMessageTime(message));
			metaLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #9399b2;");
			messageBox.getChildren().add(metaLabel);
			messageBox.getChildren().add(node);

			if (message instanceof TextMessage textMessage && textMessage.isEdited() && !textMessage.isDeleted()) {
				Label editedLabel = new Label("bearbeitet");
				editedLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #6c7086; -fx-font-style: italic;");
				messageBox.getChildren().add(editedLabel);
			}

			if (isOwn) {
				Label readStatus = new Label(getReadCheckmarks(message));
				String color = message.getReadByUsernames().isEmpty() ? "#6c7086" : "#89b4fa";
				readStatus.setStyle("-fx-font-size: 10; -fx-text-fill: " + color + ";");
				messageBox.getChildren().add(readStatus);
			}

			Node avatar = createAvatarNode(message.getSender());
			HBox container = isOwn ? new HBox(8, messageBox, avatar) : new HBox(8, avatar, messageBox);
			container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
			container.setPadding(new Insets(2, 10, 2, 10));

			if (isOwn && canShowContextMenu(message)) {
				container.setOnContextMenuRequested(event -> {
					ContextMenu contextMenu = createMessageContextMenu(message);
					contextMenu.show(container, event.getScreenX(), event.getScreenY());
				});
			}

			return container;
		}

		private Node createAvatarNode(User user) {
			byte[] imageBytes = getProfilePictureBytes(user);
			if (imageBytes != null && imageBytes.length > 0) {
				ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(imageBytes)));
				imageView.setFitWidth(32);
				imageView.setFitHeight(32);
				imageView.setPreserveRatio(false);
				return imageView;
			}

			Label fallback = new Label(getAvatarInitial(user));
			fallback.setAlignment(Pos.CENTER);
			fallback.setMinSize(32, 32);
			fallback.setPrefSize(32, 32);
			fallback.setMaxSize(32, 32);
			fallback.setStyle("-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-weight: bold; -fx-background-radius: 16;");
			return fallback;
		}

		private byte[] getProfilePictureBytes(User user) {
			if (user == null) {
				return null;
			}
			byte[] imageBytes = user.getProfilePicture();
			if (imageBytes != null && imageBytes.length > 0) {
				return imageBytes;
			}
			return profilePicturesByUsername.get(user.getUsername());
		}

		private String getAvatarInitial(User user) {
			String name = getDisplayName(user);
			return name.isBlank() ? "?" : name.substring(0, 1).toUpperCase();
		}

		private String getReadCheckmarks(Message message) {
			Set<String> readBy = message.getReadByUsernames();
			if (readBy.isEmpty()) {
				return "✓"; // Grau - nur gesendet
			} else {
				return "✓✓"; // Blau - mindestens ein Empfänger hat gelesen
			}
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
					var u = join.getUser();
					String name = "Unbekannter Nutzer";
					if (u != null) {
						// Bevorzuge den Displayname, fallback auf Username
						if (u.getDisplayname() != null && !u.getDisplayname().isBlank()) {
							name = u.getDisplayname();
						} else if (u.getUsername() != null && !u.getUsername().isBlank()) {
							name = u.getUsername();
						}
					}
					text = name + " ist beigetreten";
					color = "#89b4fa";
				}
				case LeaveNotification leave -> {
					var u = leave.getUser();
					String name = "Unbekannter Nutzer";
					if (u != null) {
						if (u.getDisplayname() != null && !u.getDisplayname().isBlank()) {
							name = u.getDisplayname();
						} else if (u.getUsername() != null && !u.getUsername().isBlank()) {
							name = u.getUsername();
						}
					}
					text = name + " hat verlassen";
					color = "#f38ba8";
				}
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + notification);
			}

			setText(text);
			setAlignment(Pos.CENTER);
			setStyle("-fx-background-color: transparent; -fx-padding: 4 0; "
				+ "-fx-text-fill: " + color + "; -fx-font-style: italic;");
		}

		private boolean canShowContextMenu(Message message) {
			if (message instanceof TextMessage textMessage) {
				return !textMessage.isDeleted();
			}
			if (message instanceof FileMessage fileMessage) {
				return !fileMessage.isDeleted();
			}
			return false;
		}

		private ContextMenu createMessageContextMenu(Message message) {
			ContextMenu menu = new ContextMenu();
			if (message instanceof FileMessage) {
				MenuItem deleteItem = new MenuItem("Loeschen");
				deleteItem.setStyle("-fx-font-size: 12;");
				deleteItem.setOnAction(event -> Controller.this.deleteMessage(message));
				menu.getItems().add(deleteItem);
				return menu;
			}

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

	private void applyProfilePictureUpdate(ProfilePictureUpdate update) {
		if (update.getUsername() == null) {
			System.err.println("ProfilePictureUpdate mit null username");
			return;
		}

		if (localUser != null && update.getUsername().equals(localUser.getUsername())) {
			localUser.setProfilePicture(update.getImageBytes());
			localUser.setProfilePictureContentType(update.getContentType());
			updateOwnProfilePictureView();
		}

		cacheProfilePicture(update.getUsername(), update.getImageBytes(), update.getContentType());

		for (Packet packet : getMessages()) {
			if (packet instanceof Message message && message.getSender() != null
				&& update.getUsername().equals(message.getSender().getUsername())) {
				message.getSender().setProfilePicture(update.getImageBytes());
				message.getSender().setProfilePictureContentType(update.getContentType());
			}
		}

		messageListView.refresh();
	}

	private void updateOwnProfilePictureView() {
		if (profilePictureView == null || localUser == null) {
			return;
		}

		byte[] imageBytes = localUser.getProfilePicture();
		if (imageBytes == null || imageBytes.length == 0) {
			profilePictureView.setImage(null);
			return;
		}

		profilePictureView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
	}

	private void cacheProfilePicture(User user) {
		if (user == null || user.getUsername() == null) {
			return;
		}
		cacheProfilePicture(user.getUsername(), user.getProfilePicture(), user.getProfilePictureContentType());
	}

	private void cacheProfilePicture(String username, byte[] imageBytes, String contentType) {
		if (username == null || imageBytes == null || imageBytes.length == 0) {
			return;
		}
		profilePicturesByUsername.put(username, imageBytes);
		profilePictureContentTypesByUsername.put(username, contentType);
	}

	private User createNetworkUser(User source) {
		if (source == null) {
			return null;
		}

		User user = new User();
		user.setUsername(source.getUsername());
		user.setDisplayname(source.getDisplayname());
		user.setStatusMessage(source.getStatusMessage());
		user.setProfileDescription(source.getProfileDescription());
		user.setProfilePictureUUID(source.getProfilePictureUUID());
		return user;
	}

	private String getDisplayName(User user) {
		if (user == null) {
			return "Unbekannt";
		}
		if (user.getDisplayname() != null && !user.getDisplayname().isBlank()) {
			return user.getDisplayname();
		}
		if (user.getUsername() != null && !user.getUsername().isBlank()) {
			return user.getUsername();
		}
		return "Unbekannt";
	}

	private String getMessageTime(Message message) {
		return message.getSentAt() == null ? "" : message.getSentAt().format(MESSAGE_TIME_FORMAT);
	}

	private void startEditMessage(TextMessage message) {
		messageTextField.setText(message.getContent());
		messageTextField.requestFocus();
		isEditingMessage = message;
		sendButton.setText("Speichern");
		sendButton.setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 70; -fx-min-height: 40;");
	}

	private void startEditMessage(Message message) {
		if (message instanceof TextMessage textMessage) {
			startEditMessage(textMessage);
		}
	}

	private void deleteMessage(TextMessage message) {
		sendPacket(new DeleteMessage(message.getMessageId()));
	}

	private void deleteMessage(Message message) {
		sendPacket(new DeleteMessage(message.getMessageId()));
	}

	//Audio
	public void stopCall() {
		if (inCall) {
			audioCall.stop();
			inCall = false;
		}
	}

	public void handleCallButton() {
		if (!inCall) {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Anruf starten");
			dialog.setHeaderText("Benutzername des Empfaengers:");
			String target = dialog.showAndWait().orElse(null);
			if (target == null || target.isBlank()) {
				return;
			}

			sendPacket(new CallNotification(
				CallNotification.CallType.REQUEST,
				createNetworkUser(localUser),
				target,
				0
			));
		} else {
			audioCall.stop();
			inCall = false;
			videoCallButton.setStyle(
				"-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 14; " +
					"-fx-background-radius: 20; -fx-min-width: 70; -fx-min-height: 40;"
			);
		}
	}

	private void handleCallNotification(CallNotification call) {
		if (localUser == null || call.getTargetUsername() == null || !call.getTargetUsername().equals(localUser.getUsername())) {
			return;
		}

		switch (call.getType()) {
			case REQUEST -> {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Eingehender Anruf");
				alert.setHeaderText("Anruf von: " + call.getSender().getUsername());
				boolean accepted = alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
				sendPacket(new CallNotification(
					accepted ? CallNotification.CallType.ACCEPT : CallNotification.CallType.REJECT,
					createNetworkUser(localUser),
					call.getSender().getUsername(),
					0
				));
				if (accepted) {
					startAudioCall(call);
				}
			}
			case ACCEPT -> startAudioCall(call);
			case REJECT -> getMessages().add(
				new TextMessage(createNetworkUser(localUser), call.getSender().getUsername() + " hat abgelehnt."));
		}
	}

	private void startAudioCall(CallNotification call) {
		String roomId = Stream.of(localUser.getUsername(), call.getSender().getUsername())
			.sorted()
			.collect(Collectors.joining("-"));
		try {
			audioCall.start(RELAY_IP, RELAY_PORT, roomId);
			inCall = true;
			videoCallButton.setStyle(
				"-fx-background-color: #f38ba8; -fx-text-fill: #1e1e2e; -fx-font-size: 14; " +
					"-fx-background-radius: 20; -fx-min-width: 70; -fx-min-height: 40;"
			);
		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Anruf konnte nicht gestartet werden");
			alert.setContentText(e.toString());
			alert.show();
		}
	}


}
