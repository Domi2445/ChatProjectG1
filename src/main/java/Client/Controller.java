package Client;

import AudioCall.AudioCall;
import VideoCall.VideoCall;
import User.Login.Status;
import User.Model.User;
import User.Model.ChatMessage;
import User.Model.MessageType;
import Util.Emoji.Emoji;
import Util.Emoji.EmojiService;
import Util.FileUtil;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.LoginResponse;
import Util.Network.Auth.RegisterRequest;
import Util.Network.Auth.RegisterResponse;
import Util.Network.ConnectionClosed;
import Util.Network.DeleteForMeMessage;
import Util.Network.DeleteMessage;
import Util.Network.Groups.CreateGroupPacket;
import Util.Network.Groups.Group;
import Util.Network.Groups.GroupCreatedPacket;
import Util.Network.Groups.GroupListResponsePacket;
import Util.Network.Groups.JoinGroupPacket;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
	// null = globaler Chat, sonst Username des privaten Gesprächspartners
	private String activePrivateChat = null;
	// null = kein Gruppen-Chat aktiv
	private Group activeGroup = null;

	private final ObservableList<ChatEntry> chatList = FXCollections.observableArrayList();
	private final Map<String, byte[]> profilePicturesByUsername = new HashMap<>();
	private final Map<String, String> profilePictureContentTypesByUsername = new HashMap<>();
	private boolean profilePictureSyncEnabled;
	private boolean historyLoaded = false;
	// Relay (Audio/Video) läuft im selben Server-Prozess wie der Chat – Host wird beim Verbinden gesetzt.
	private String relayHost;
	private static final String RELAY_IP = "217.154.156.40";
	private static final int RELAY_PORT = 3268;
	private static final int VIDEO_RELAY_PORT = 9001;
	private final AudioCall audioCall = new AudioCall();
	private final VideoCall videoCall = new VideoCall();
	private boolean inCall = false;
	// Anruf wurde initiiert, aber noch nicht von der Gegenseite angenommen (für rotes Button-Feedback)
	private boolean pendingCall = false;
	// Art des aktiven/ausstehenden Anrufs: true = Video, false = nur Audio
	private boolean callIsVideo = false;
	// Benutzername der Gegenseite des aktuellen/ausstehenden Anrufs (für "Auflegen"-Signal)
	private String callPeer = null;

	private final ObservableList<User> userList = FXCollections.observableArrayList();
	private final FilteredList<User> filteredUserList = new FilteredList<>(userList, p -> true);

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

	@FXML
	private ImageView remoteVideoView;

	@FXML
	private Button videoButton;

	@FXML
	private Button buttonEmoji;

	@FXML
	private Label welcomeLabel;

	@FXML
	private Label chatHeaderLabel;

	@FXML
	private ListView<ChatEntry> chatListView;

	@FXML
	private Button createGroupButton;

	@FXML
	private ListView<User> userListView;

	@FXML
	private TextField textFieldSearch;

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
		uploadButton.setOnAction(e -> openUploadMenu());
		videoCallButton.setOnAction(e -> handleCallButton());

		if (buttonEmoji != null) {
			buttonEmoji.setOnAction(e -> openEmojiPicker());
		}
		if (videoButton != null) {
			videoButton.setOnAction(e -> handleVideoButton());
		}

		if (userListView != null) {
			userListView.setItems(filteredUserList);
			userListView.setCellFactory(lv -> new UserCell());
			userListView.setOnMouseClicked(e -> {
				User selected = userListView.getSelectionModel().getSelectedItem();
				if (selected != null) openChat(selected);
			});
		}

		if (chatListView != null) {
			chatListView.setItems(chatList);
			chatListView.setCellFactory(lv -> new ChatEntryCell());
			chatListView.setOnMouseClicked(e -> {
				ChatEntry selected = chatListView.getSelectionModel().getSelectedItem();
				if (selected != null) openChatEntry(selected);
			});
			// Globaler Chat ist immer vorhanden
			chatList.add(ChatEntry.global());
		}

		if (createGroupButton != null) {
			createGroupButton.setOnAction(e -> showCreateGroupDialog());
		}

		if (textFieldSearch != null) {
			textFieldSearch.textProperty().addListener((obs, oldVal, newVal) -> applyUserFilter(newVal));
		}
	}

	private void openUploadMenu() {
		ContextMenu menu = new ContextMenu();
		MenuItem fileItem = new MenuItem("Datei senden");
		fileItem.setOnAction(e -> sendFile());
		MenuItem profileItem = new MenuItem("Profilbild aendern");
		profileItem.setOnAction(e -> uploadProfilePicture());
		menu.getItems().addAll(fileItem, profileItem);
		menu.show(uploadButton, Side.TOP, 0, 0);
	}

	public void configure(Stage stage, User user) {
		this.stage = stage;
		this.localUser = user;
	}

	public boolean connectAndRun(String ip, int port) {
		try {
			profilePictureSyncEnabled = Boolean.parseBoolean(System.getProperty("profile.sync", "true"));
			this.relayHost = ip;
			profilePictureSyncEnabled = Boolean.parseBoolean(System.getProperty("profile.sync", "false"));
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
								if (isMessageForActiveChat(message)) {
									getMessages().add(message);
									if (localUser != null && message.getSender() != null && !message.getSender().equals(localUser)) {
										outPacketQueue.offer(new Util.Network.ReadReceipt(message.getMessageId(), localUser.getUsername()));
									}
									messageListView.scrollTo(getMessages().size() - 1);
								}
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
							case GroupListResponsePacket groupList -> Platform.runLater(() -> {
								for (Group g : groupList.getGroups()) {
									addGroupToChatList(g);
								}
							});
							case GroupCreatedPacket created -> Platform.runLater(() -> {
								addGroupToChatList(created.getGroup());
								openGroupChat(created.getGroup());
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
									// Konvertiere ChatMessage in Message/TextMessage bzw. FileMessage für die UI
									var fileContents = histResp.getFileContents();
									var fileExtensions = histResp.getFileExtensions();
									for (ChatMessage dbMsg : histResp.getMessages()) {
										if (dbMsg.getMessageType() == MessageType.TEXT || dbMsg.getMessageType() == MessageType.EMOJI) {
											TextMessage msg = new TextMessage(new User(dbMsg.getSender()), dbMsg.getContent(), dbMsg.getId(), dbMsg.getTimestamp());
											if (dbMsg.isDeleted()) msg.setDeleted();
											getMessages().add(msg);
										} else if (dbMsg.getMessageType() == MessageType.FILE) {
											String fileId = dbMsg.getFilePath();
											if (fileId != null && fileContents != null && fileContents.containsKey(fileId)) {
												byte[] bytes = fileContents.get(fileId);
												String ext = fileExtensions != null ? fileExtensions.getOrDefault(fileId, "bin") : "bin";
												FileMessage msg = new FileMessage(new User(dbMsg.getSender()), bytes, ext, dbMsg.getId(), dbMsg.getTimestamp());
												if (dbMsg.isDeleted()) msg.setDeleted();
												getMessages().add(msg);
											} else {
												// Fallback: keine Dateibytes verfügbar -> Platzhaltertext
												Message msg = new TextMessage(new User(dbMsg.getSender()), "[Datei: " + dbMsg.getFilePath() + "]");
												getMessages().add(msg);
											}
										}
									}
									messageListView.refresh();
									System.out.println("Chat-History geladen: " + histResp.getMessages().size() + " Messages");
								} else {
									System.err.println("Fehler beim Laden der History: " + histResp.getErrorMessage());
								}
							});
case null, default -> System.err.println("Unbekanntes Paket empfangen: " + (packet == null ? "null" : packet.getClass().getName()));
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

			var response = Main.themed(alert).showAndWait();
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
			Main.themed(alert).show();
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
				TextMessage message = new TextMessage(createNetworkUser(localUser), text);
				if (activePrivateChat != null) {
					message.setReceiverUsername(activePrivateChat);
				} else if (activeGroup != null) {
					message.setGroupId(activeGroup.getId());
				}
				if (!sendPacket(message)) {
					return;
				}
			}

			messageListView.scrollTo(getMessages().size() - 1);
			messageTextField.clear();
		}
	}

	private void resetSendButton() {
		sendButton.setText("Senden");
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
			Main.themed(alert).show();
			return;
		}

		byte[] bytes;

		try {
			bytes = Files.readAllBytes(selectedFile.toPath());
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Datei konnte nicht geöffnet werden");
			alert.setContentText(e.toString());
			Main.themed(alert).show();
			return;
		}

		String fileName = selectedFile.getName();
		String fileExt = FileUtil.getFileExtension(fileName).toLowerCase();
		FileMessage message = new FileMessage(createNetworkUser(localUser), bytes, fileExt);
		if (activePrivateChat != null) {
			message.setReceiverUsername(activePrivateChat);
		} else if (activeGroup != null) {
			message.setGroupId(activeGroup.getId());
		}

		if (!sendPacket(message)) {
			return;
		}

		messageListView.scrollTo(getMessages().size() - 1);
		messageTextField.clear();
	}

	private void saveFileMessage(FileMessage fileMessage) {
		FileChooser chooser = new FileChooser();
		String extension = fileMessage.getFileExtension();
		chooser.setInitialFileName("datei." + extension);
		if (extension != null && !extension.isBlank()) {
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension + "-Datei", "*." + extension));
		}
		File target = chooser.showSaveDialog(stage);
		if (target == null) {
			return;
		}
		try {
			Files.write(target.toPath(), fileMessage.getContent());
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Datei konnte nicht gespeichert werden");
			alert.setContentText(e.toString());
			Main.themed(alert).show();
		}
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
			Main.themed(alert).show();
			return;
		}

		String extension = FileUtil.getFileExtension(selectedFile.getName()).toLowerCase();
		if (!FileUtil.isImageExtension(extension)) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Bitte eine Bilddatei auswaehlen");
			Main.themed(alert).show();
			return;
		}

		try {
			byte[] bytes = createProfilePictureBytes(selectedFile);
			if (bytes.length > MAX_PROFILE_PICTURE_SIZE) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setHeaderText("Profilbild konnte nicht verkleinert werden");
				alert.setContentText(bytes.length + " Bytes / " + MAX_PROFILE_PICTURE_SIZE + " Bytes");
				Main.themed(alert).show();
				return;
			}
			localUser.setProfilePicture(bytes);
			localUser.setProfilePictureContentType("image/jpeg");
			cacheProfilePicture(localUser.getUsername(), bytes, localUser.getProfilePictureContentType());
			messageListView.refresh();
			if (userListView != null) {
				userListView.refresh();
			}
			if (profilePictureSyncEnabled) {
				sendPacket(new ProfilePictureUpdate(localUser.getUsername(), bytes, "image/jpeg"));
			}
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Profilbild konnte nicht geoeffnet werden");
			alert.setContentText(e.toString());
			Main.themed(alert).show();
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
				cacheProfilePicture(join.getUser());
				addUserToList(join.getUser());
			}
			case LeaveNotification leave -> removeUserFromList(leave.getUser());
			case null, default -> throw new IllegalStateException("Unbekannte Systemnachricht");
		}
	}

	private void handleConnectionClosed(ConnectionClosed closed) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("Verbindung zum Server getrennt");
		alert.setContentText(closed.getReason());
		Main.themed(alert).show();
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
			updateWelcomeLabel();
			addUserToList(localUser);
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
			updateWelcomeLabel();
			addUserToList(localUser);
		}

		if (onRegisterResult != null) {
			onRegisterResult.accept(response);
		}
	}

	private void updateWelcomeLabel() {
		if (welcomeLabel == null || localUser == null) {
			return;
		}
		welcomeLabel.setText("Willkommen, " + getDisplayName(localUser) + "!");
	}

	private void addUserToList(User user) {
		if (user == null || user.getUsername() == null) {
			return;
		}
		for (User existing : userList) {
			if (user.getUsername().equals(existing.getUsername())) {
				return;
			}
		}
		userList.add(user);
	}

	private void removeUserFromList(User user) {
		if (user == null || user.getUsername() == null) {
			return;
		}
		userList.removeIf(existing -> user.getUsername().equals(existing.getUsername()));
	}

	private void openChat(User user) {
		if (localUser == null) return;

		if (user.getUsername().equals(localUser.getUsername())) {
			// Klick auf sich selbst → globaler Chat
			openGlobalChat();
			return;
		}

		activePrivateChat = user.getUsername();
		activeGroup = null;
		addPrivateChatToChatList(user.getUsername(), getDisplayName(user));
		if (chatHeaderLabel != null) chatHeaderLabel.setText(getDisplayName(user));
		getMessages().clear();
		outPacketQueue.offer(new HistoryRequest(localUser.getUsername(), user.getUsername(), null));
	}

	private boolean isMessageForActiveChat(Message message) {
		String receiver = message.getReceiverUsername();
		UUID msgGroupId = message.getGroupId();
		String senderName = message.getSender() != null ? message.getSender().getUsername() : null;
		String myName = localUser != null ? localUser.getUsername() : null;

		if (activeGroup != null) {
			return activeGroup.getId().equals(msgGroupId);
		} else if (activePrivateChat != null) {
			return (activePrivateChat.equals(receiver) && myName != null && myName.equals(senderName))
				|| (myName != null && myName.equals(receiver) && activePrivateChat.equals(senderName));
		} else {
			// Globaler Chat: keine groupId, kein receiver
			return msgGroupId == null && receiver == null;
		}
	}

	private void openGlobalChat() {
		activePrivateChat = null;
		activeGroup = null;
		if (chatHeaderLabel != null) chatHeaderLabel.setText("Globaler Chat");
		getMessages().clear();
		outPacketQueue.offer(new HistoryRequest(localUser.getUsername(), null, "main"));
	}

	private void openGroupChat(Group group) {
		activePrivateChat = null;
		activeGroup = group;
		if (chatHeaderLabel != null) chatHeaderLabel.setText(group.getName());
		getMessages().clear();
		outPacketQueue.offer(new HistoryRequest(localUser.getUsername(), null, group.getId().toString()));
	}

	private void openChatEntry(ChatEntry entry) {
		if (localUser == null) return;
		switch (entry.type()) {
			case GLOBAL -> openGlobalChat();
			case GROUP -> {
				// Gruppe beitreten falls noch nicht Mitglied, dann öffnen
				outPacketQueue.offer(new JoinGroupPacket(entry.groupId()));
				openGroupChat(new Group(entry.groupId(), entry.displayName(), ""));
			}
			case PRIVATE -> {
				activePrivateChat = entry.username();
				activeGroup = null;
				if (chatHeaderLabel != null) chatHeaderLabel.setText(entry.displayName());
				getMessages().clear();
				outPacketQueue.offer(new HistoryRequest(localUser.getUsername(), entry.username(), null));
			}
		}
	}

	private void addGroupToChatList(Group group) {
		for (ChatEntry e : chatList) {
			if (e.type() == ChatEntry.Type.GROUP && group.getId().equals(e.groupId())) return;
		}
		chatList.add(ChatEntry.group(group));
	}

	private void addPrivateChatToChatList(String username, String displayName) {
		for (ChatEntry e : chatList) {
			if (e.type() == ChatEntry.Type.PRIVATE && username.equals(e.username())) return;
		}
		chatList.add(ChatEntry.privateDm(username, displayName));
	}

	private void showCreateGroupDialog() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Gruppe erstellen");
		dialog.setHeaderText("Name der neuen Gruppe:");
		Main.themed(dialog).showAndWait().ifPresent(name -> {
			if (!name.isBlank()) {
				outPacketQueue.offer(new CreateGroupPacket(name.trim()));
			}
		});
	}

	private void showJoinGroupDialog() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Gruppe beitreten");
		dialog.setHeaderText("Gruppen-ID eingeben:");
		Main.themed(dialog).showAndWait().ifPresent(idStr -> {
			try {
				UUID groupId = UUID.fromString(idStr.trim());
				outPacketQueue.offer(new JoinGroupPacket(groupId));
			} catch (IllegalArgumentException ex) {
				Alert alert = new Alert(Alert.AlertType.ERROR, "Ungültige Gruppen-ID");
				Main.themed(alert).show();
			}
		});
	}

	private void applyUserFilter(String search) {
		if (search == null || search.isBlank()) {
			filteredUserList.setPredicate(u -> true);
			return;
		}
		String needle = search.toLowerCase();
		filteredUserList.setPredicate(u -> {
			if (u == null) return false;
			if (u.getUsername() != null && u.getUsername().toLowerCase().contains(needle)) return true;
			if (u.getDisplayname() != null && u.getDisplayname().toLowerCase().contains(needle)) return true;
			return false;
		});
	}

	private class MessageCell extends ListCell<Packet> {
		private TextMessage editingMessage;

		@Override
		protected void updateItem(Packet item, boolean empty) {
			super.updateItem(item, empty);

			setText(null);
			setGraphic(null);
			if (!getStyleClass().contains("message-cell")) {
				getStyleClass().add("message-cell");
			}

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
						label.getStyleClass().add("deleted-label");
					}

					node = label;
				}
				case FileMessage fileMessage -> {
					if (fileMessage.isDeleted()) {
						Label label = new Label("Diese Datei wurde geloescht");
						label.setWrapText(true);
						label.setMaxWidth(300);
						label.getStyleClass().add("deleted-label");
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
			node.getStyleClass().add(isOwn ? "bubble-own" : "bubble-other");

			VBox messageBox = new VBox(2);
			Label metaLabel = new Label(getDisplayName(message.getSender()) + "  " + getMessageTime(message));
			metaLabel.getStyleClass().add("meta-label");
			messageBox.getChildren().add(metaLabel);
			messageBox.getChildren().add(node);

			if (message instanceof TextMessage textMessage && textMessage.isEdited() && !textMessage.isDeleted()) {
				Label editedLabel = new Label("bearbeitet");
				editedLabel.getStyleClass().add("edited-label");
				messageBox.getChildren().add(editedLabel);
			}

			if (isOwn) {
				Label readStatus = new Label(getReadCheckmarks(message));
				readStatus.getStyleClass().add(message.getReadByUsernames().isEmpty() ? "read-status" : "read-status-read");
				messageBox.getChildren().add(readStatus);
			}

			Node avatar = createAvatarNode(message.getSender());
			HBox container = isOwn ? new HBox(8, messageBox, avatar) : new HBox(8, avatar, messageBox);
			container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
			container.setPadding(new Insets(2, 10, 2, 10));

			if (canShowContextMenu(message)) {
				container.setOnContextMenuRequested(event -> {
					ContextMenu contextMenu = createMessageContextMenu(message, isOwn);
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
			fallback.getStyleClass().add("avatar-fallback");
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
					() -> Math.clamp(getScene().getWidth() - 32, 100.0, Math.max(100.0, image.getWidth())),
					getScene().widthProperty()
				));
				return imageView;
			} else {
				Label label = new Label(fileMessage.getFileExtension() + "-Datei");
				Button saveButton = new Button("Speichern");
				saveButton.getStyleClass().add("rounded");
				saveButton.setOnAction(e -> Controller.this.saveFileMessage(fileMessage));
				HBox box = new HBox(8, label, saveButton);
				box.setAlignment(Pos.CENTER_LEFT);
				return box;
			}
		}

		private void renderNotificationLine(Notification notification) {
			String text;

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
				}
				case null, default -> throw new IllegalStateException("Unerwarteter Wert: " + notification);
			}

			setText(text);
			setAlignment(Pos.CENTER);
			if (!getStyleClass().contains("notification-line")) {
				getStyleClass().add("notification-line");
			}
		}

		private boolean canShowContextMenu(Message message) {
			// Für alle gelöschte Nachrichten: kein Menü mehr nötig
			if (message instanceof TextMessage tm) return !tm.isDeleted();
			if (message instanceof FileMessage fm) return !fm.isDeleted();
			return false;
		}

		private ContextMenu createMessageContextMenu(Message message, boolean isOwn) {
			ContextMenu menu = new ContextMenu();

			MenuItem deleteForMeItem = new MenuItem("Nur für mich löschen");
			deleteForMeItem.setOnAction(event -> {
				sendPacket(new DeleteForMeMessage(message.getMessageId()));
				getMessages().remove(message);
				messageListView.refresh();
			});

			menu.getItems().add(deleteForMeItem);

			if (isOwn) {
				MenuItem deleteForAllItem = new MenuItem("Für alle löschen");
				deleteForAllItem.setOnAction(event -> Controller.this.deleteMessage(message));
				menu.getItems().add(deleteForAllItem);

				if (message instanceof TextMessage) {
					MenuItem editItem = new MenuItem("✏️ Bearbeiten");
					editItem.setStyle("-fx-font-size: 12;");
					editItem.setOnAction(event -> Controller.this.startEditMessage(message));
					menu.getItems().add(0, editItem);
				}
			}

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
		if (inCall || pendingCall) {
			endCall();
		}
	}

	// "Anruf"-Button → reiner Audioanruf
	public void handleCallButton() {
		if (inCall || pendingCall) {
			endCall();
		} else {
			// Wenn ein privater Chat offen ist, direkt diesen Kontakt anrufen (kein Dialog nötig)
			startOutgoingCall(false, activePrivateChat);
		}
	}

	// "Videoanruf"-Button → Audio + Video
	private void handleVideoButton() {
		if (inCall || pendingCall) {
			endCall();
		} else {
			// Wenn ein privater Chat offen ist, direkt diesen Kontakt anrufen (kein Dialog nötig)
			startOutgoingCall(true, activePrivateChat);
		}
	}

	// Anruf an einen Benutzernamen starten (target == null fragt per Dialog nach)
	private void startOutgoingCall(boolean video) {
		startOutgoingCall(video, null);
	}

	private void startOutgoingCall(boolean video, String target) {
		if (target == null || target.isBlank()) {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle(video ? "Videoanruf starten" : "Anruf starten");
			dialog.setHeaderText("Benutzername des Empfaengers:");
			target = Main.themed(dialog).showAndWait().orElse(null);
			if (target == null || target.isBlank()) {
				return;
			}
		}

		callIsVideo = video;
		callPeer = target;
		pendingCall = true;
		setCallButtonActive(video, true);
		System.out.println("[Call] REQUEST gesendet an " + target + " (video=" + video + ")");

		sendPacket(new CallNotification(
			CallNotification.CallType.REQUEST,
			createNetworkUser(localUser),
			target,
			0,
			video
		));
	}

	private void endCall() {
		// Gegenseite benachrichtigen, damit der Anruf dort ebenfalls beendet wird
		// (Buttons zurücksetzen, Audio/Video stoppen).
		if (callPeer != null) {
			sendPacket(new CallNotification(
				CallNotification.CallType.END,
				createNetworkUser(localUser),
				callPeer,
				0,
				callIsVideo
			));
		}
		stopCallLocally();
	}

	// Beendet den Anruf nur lokal, ohne die Gegenseite zu benachrichtigen.
	private void stopCallLocally() {
		audioCall.stop();
		videoCall.stop();
		resetCallState();
	}

	// Setzt alle Anruf-Zustände zurück und entfernt die rote Markierung von beiden Buttons.
	private void resetCallState() {
		inCall = false;
		pendingCall = false;
		callPeer = null;
		videoCallButton.getStyleClass().remove("call-active");
		videoButton.getStyleClass().remove("call-active");
	}

	// Markiert den zur Anrufart passenden Button rot (call-active) bzw. entfernt die Markierung.
	private void setCallButtonActive(boolean video, boolean active) {
		Button button = video ? videoButton : videoCallButton;
		if (active) {
			if (!button.getStyleClass().contains("call-active")) {
				button.getStyleClass().add("call-active");
			}
		} else {
			button.getStyleClass().remove("call-active");
		}
	}

	private void handleCallNotification(CallNotification call) {
		if (localUser == null || call.getTargetUsername() == null || !call.getTargetUsername().equals(localUser.getUsername())) {
			return;
		}

		switch (call.getType()) {
			case REQUEST -> {
				System.out.println("[Call] REQUEST empfangen von " + call.getSender().getUsername() + " (video=" + call.isVideo() + ")");
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Eingehender Anruf");
				alert.setHeaderText((call.isVideo() ? "Videoanruf" : "Anruf") + " von: " + call.getSender().getUsername());
				boolean accepted = Main.themed(alert).showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
				sendPacket(new CallNotification(
					accepted ? CallNotification.CallType.ACCEPT : CallNotification.CallType.REJECT,
					createNetworkUser(localUser),
					call.getSender().getUsername(),
					0,
					call.isVideo()
				));
				if (accepted) {
					startCall(call, call.isVideo());
				}
			}
			case ACCEPT -> {
				System.out.println("[Call] ACCEPT empfangen von " + call.getSender().getUsername() + " (video=" + call.isVideo() + ")");
				startCall(call, call.isVideo());
			}
			case REJECT -> {
				System.out.println("[Call] REJECT empfangen von " + call.getSender().getUsername());
				resetCallState();
				getMessages().add(
					new TextMessage(createNetworkUser(localUser), call.getSender().getUsername() + " hat abgelehnt."));
			}
			case END -> {
				System.out.println("[Call] END empfangen von " + call.getSender().getUsername());
				if (inCall || pendingCall) {
					stopCallLocally();
					getMessages().add(new TextMessage(createNetworkUser(localUser),
						call.getSender().getUsername() + " hat den Anruf beendet."));
				}
			}
		}
	}

	private void startCall(CallNotification call, boolean video) {
		String roomId = Stream.of(localUser.getUsername(), call.getSender().getUsername())
			.sorted()
			.collect(Collectors.joining("-"));
		System.out.println("[Call] Starte " + (video ? "Video+Audio" : "Audio") + " | relay=" + relayHost + " | room=" + roomId);
		try {
			audioCall.start(relayHost, RELAY_PORT, roomId);
			if (video) {
				videoCall.start(relayHost, VIDEO_RELAY_PORT, roomId, remoteVideoView);
			}
			callIsVideo = video;
			callPeer = call.getSender().getUsername();
			inCall = true;
			pendingCall = false;
			setCallButtonActive(video, true);
		} catch (Exception e) {
			System.err.println("[Call] Fehler beim Start: " + e);
			resetCallState();
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Anruf konnte nicht gestartet werden");
			alert.setContentText(e.toString());
			Main.themed(alert).show();
		}
	}

	private void openEmojiPicker() {
		Stage pickerStage = new Stage();
		pickerStage.setTitle("Emoji auswaehlen");

		FlowPane root = new FlowPane(8, 8);
		root.setPadding(new Insets(12));
		Label loading = new Label("Lade Emojis...");
		loading.getStyleClass().add("meta-label");
		root.getChildren().add(loading);

		Scene scene = new Scene(root, 360, 280);
		scene.getStylesheets().add(Main.CUPERTINO_DARK_CSS);
		pickerStage.setScene(scene);
		pickerStage.show();

		Thread loader = new Thread(() -> {
			java.util.List<Emoji> emojis = new EmojiService().loadEmojis();
			Platform.runLater(() -> {
				root.getChildren().clear();
				if (emojis.isEmpty()) {
					Label empty = new Label("Keine Emojis verfügbar");
					empty.getStyleClass().add("meta-label");
					root.getChildren().add(empty);
					return;
				}
				for (Emoji emoji : emojis) {
					Button btn = new Button(emoji.getCharacter());
					btn.getStyleClass().add("rounded");
					btn.setOnAction(ev -> {
						messageTextField.appendText(emoji.getCharacter());
						pickerStage.close();
					});
					root.getChildren().add(btn);
				}
			});
		}, "EmojiLoader");
		loader.setDaemon(true);
		loader.start();
	}

	public record ChatEntry(Type type, String displayName, UUID groupId, String username) {
		public enum Type { GLOBAL, GROUP, PRIVATE }

		public static ChatEntry global() {
			return new ChatEntry(Type.GLOBAL, "💬 Globaler Chat", null, null);
		}

		public static ChatEntry group(Group g) {
			return new ChatEntry(Type.GROUP, g.getName(), g.getId(), null);
		}

		public static ChatEntry privateDm(String username, String displayName) {
			return new ChatEntry(Type.PRIVATE, "👤 " + displayName, null, username);
		}

		@Override public String toString() { return displayName; }
	}

	private class ChatEntryCell extends ListCell<ChatEntry> {
		@Override
		protected void updateItem(ChatEntry item, boolean empty) {
			super.updateItem(item, empty);
			setText(null);
			setGraphic(null);
			if (empty || item == null) return;

			Label label = new Label(item.displayName());
			label.getStyleClass().add("user-cell-name");

			HBox box = new HBox(8, label);
			box.setAlignment(Pos.CENTER_LEFT);
			box.setPadding(new Insets(6, 10, 6, 10));

			if (item.type() == ChatEntry.Type.GROUP) {
				Button joinBtn = new Button("ID");
				joinBtn.setStyle("-fx-font-size: 9;");
				joinBtn.setOnAction(e -> {
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setHeaderText("Gruppen-ID");
					alert.setContentText(item.groupId().toString());
					Main.themed(alert).show();
				});
				box.getChildren().add(joinBtn);
			}

			setGraphic(box);
		}
	}

	private class UserCell extends ListCell<User> {
		@Override
		protected void updateItem(User user, boolean empty) {
			super.updateItem(user, empty);
			setText(null);
			setGraphic(null);
			if (!getStyleClass().contains("user-cell")) {
				getStyleClass().add("user-cell");
			}
			if (empty || user == null) {
				return;
			}

			HBox container = new HBox(8, createUserAvatar(user), buildNameLabel(user));
			container.setAlignment(Pos.CENTER_LEFT);
			container.setPadding(new Insets(6, 10, 6, 10));
			setGraphic(container);
		}

		private Label buildNameLabel(User user) {
			Label label = new Label(getDisplayName(user));
			label.getStyleClass().add("user-cell-name");
			return label;
		}

		private Node createUserAvatar(User user) {
			byte[] imageBytes = user.getProfilePicture();
			if (imageBytes == null || imageBytes.length == 0) {
				imageBytes = profilePicturesByUsername.get(user.getUsername());
			}
			if (imageBytes != null && imageBytes.length > 0) {
				ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(imageBytes)));
				imageView.setFitWidth(28);
				imageView.setFitHeight(28);
				imageView.setPreserveRatio(false);
				imageView.setClip(new Circle(14, 14, 14));
				return imageView;
			}
			String name = getDisplayName(user);
			Label fallback = new Label(name.isBlank() ? "?" : name.substring(0, 1).toUpperCase());
			fallback.setAlignment(Pos.CENTER);
			fallback.setMinSize(28, 28);
			fallback.setPrefSize(28, 28);
			fallback.setMaxSize(28, 28);
			fallback.getStyleClass().add("avatar-fallback");
			return fallback;
		}
	}
}
