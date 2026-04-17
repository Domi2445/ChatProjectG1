package Client;

import Util.Emoji.EmojiMessage;
import Util.Message.FileMessage;
import Util.Message.Message;
import Util.Message.TextMessage;
import Util.User;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
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

public class Controller {
	public static final int MAX_FILE_SIZE = 1_000_000;

	private final BlockingQueue<Message> outgoingMessageQueue;
	private final BlockingQueue<Message> incomingMessageQueue;

	private Client client;
	private User localUser;
	private Stage stage;
	private TextMessage editingMessage;

	@FXML
	private ListView<Message> messageListView;

	@FXML
	private TextField messageTextField;

	@FXML
	private Button sendButton;

	@FXML
	private Button uploadButton;

	public Controller() {
		this.outgoingMessageQueue = new ArrayBlockingQueue<>(4);
		this.incomingMessageQueue = new ArrayBlockingQueue<>(4);
	}

	@FXML
	private void initialize() {
		messageListView.setCellFactory(listView -> new MessageCell());
		sendButton.setText("Senden");
		sendButton.setOnAction(event -> sendMessage());
		messageTextField.setOnAction(event -> sendMessage());
		uploadButton.setOnAction(event -> sendFile());
	}

	public void configure(Stage stage, User user) {
		this.stage = stage;
		this.localUser = user;
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
						Platform.runLater(() -> showIncomingMessage(message));
					} catch (InterruptedException e) {
						break;
					}
				}
			}, "IncomingMessageListener");
			listener.setDaemon(true);
			listener.start();
		} catch (IOException e) {
			Platform.runLater(() -> showIncomingMessage(
					new TextMessage(new User("System"), "Verbindung fehlgeschlagen: " + e.getMessage())
			));
		}
	}

	private ObservableList<Message> getMessages() {
		return messageListView.getItems();
	}

	private void showIncomingMessage(Message message) {
		int index = findMessageIndex(message.getMessageId());

		if (index >= 0) {
			getMessages().set(index, message);
			messageListView.scrollTo(index);
			return;
		}

		getMessages().add(message);
		messageListView.scrollTo(getMessages().size() - 1);
	}

	private int findMessageIndex(String messageId) {
		for (int i = 0; i < getMessages().size(); i++) {
			if (getMessages().get(i).getMessageId().equals(messageId)) {
				return i;
			}
		}

		return -1;
	}

	private void sendMessage() {
		String text = messageTextField.getText().trim();
		if (text.isEmpty()) {
			return;
		}

		TextMessage message;
		if (editingMessage == null) {
			message = new TextMessage(localUser, text);
		} else {
			message = new TextMessage(editingMessage.getMessageId(), localUser, text, true);
		}

		sendToQueue(message);
		stopEditing();
	}

	private void sendFile() {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(stage);

		if (selectedFile == null || !selectedFile.isFile()) {
			return;
		}

		if (selectedFile.length() > MAX_FILE_SIZE) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Die Datei ist zu groß");
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

		sendToQueue(new FileMessage(localUser, bytes, fileName, fileType));
		stopEditing();
	}

	public void sendEmojiMessage(User user, String emoji) {
		sendToQueue(new EmojiMessage(user, emoji));
		stopEditing();
	}

	private void sendToQueue(Message message) {
		try {
			outgoingMessageQueue.put(message);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void startEditing(TextMessage textMessage) {
		editingMessage = textMessage;
		messageTextField.setText(textMessage.getContent());
		sendButton.setText("Speichern");
		messageTextField.requestFocus();
		messageTextField.positionCaret(messageTextField.getText().length());
	}

	private void stopEditing() {
		editingMessage = null;
		messageTextField.clear();
		sendButton.setText("Senden");
	}

	private boolean isOwnMessage(Message message) {
		return localUser != null
				&& message.getSender().getIdentifier().equals(localUser.getIdentifier());
	}

	private class MessageCell extends ListCell<Message> {
		@Override
		protected void updateItem(Message item, boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null) {
				setGraphic(null);
				setContextMenu(null);
				setStyle("-fx-background-color: transparent;");
				return;
			}

			boolean isOwn = isOwnMessage(item);
			Node node = createNode(item, isOwn);
			styleBubble(node, isOwn);

			HBox container = new HBox(node);
			container.setPadding(new Insets(2, 10, 2, 10));
			container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

			setGraphic(container);
			setContextMenu(createContextMenu(item, isOwn));
			setStyle("-fx-background-color: transparent; -fx-padding: 0;");
		}

		private Node createNode(Message item, boolean isOwn) {
			return switch (item) {
				case TextMessage textMessage -> createTextNode(textMessage, isOwn);
				case FileMessage fileMessage -> createFileNode(fileMessage, isOwn);
				case EmojiMessage emojiMessage -> createEmojiNode(emojiMessage, isOwn);
				default -> new Label("Unbekannte Nachricht");
			};
		}

		private Node createTextNode(TextMessage textMessage, boolean isOwn) {
			Label textLabel = new Label(textMessage.getContent());
			textLabel.setWrapText(true);
			textLabel.setMaxWidth(300);
			styleText(textLabel, isOwn, 13);

			if (!textMessage.isEdited()) {
				return textLabel;
			}

			Label editedLabel = new Label("bearbeitet");
			styleText(editedLabel, isOwn, 10);

			VBox box = new VBox(4, textLabel, editedLabel);
			box.setMaxWidth(300);
			return box;
		}

		private Node createFileNode(FileMessage fileMessage, boolean isOwn) {
			if (fileMessage.getFileType() == FileMessage.FileType.IMAGE) {
				Image image = new Image(new ByteArrayInputStream(fileMessage.getContent()));
				ImageView imageView = new ImageView(image);
				imageView.setPreserveRatio(true);
				imageView.setFitWidth(250);
				return imageView;
			}

			Label label = new Label("Datei: " + fileMessage.getFileName());
			styleText(label, isOwn, 13);
			return label;
		}

		private Node createEmojiNode(EmojiMessage emojiMessage, boolean isOwn) {
			Label label = new Label(emojiMessage.getEmoji());
			styleText(label, isOwn, 24);
			return label;
		}

		private void styleBubble(Node node, boolean isOwn) {
			if (isOwn) {
				node.setStyle("-fx-background-color: #89b4fa; -fx-padding: 8 12; -fx-background-radius: 14 14 4 14;");
			} else {
				node.setStyle("-fx-background-color: #313244; -fx-padding: 8 12; -fx-background-radius: 14 14 14 4;");
			}
		}

		private void styleText(Label label, boolean isOwn, int fontSize) {
			String textColor = isOwn ? "#1e1e2e" : "#cdd6f4";
			label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: " + fontSize + ";");
		}

		private ContextMenu createContextMenu(Message item, boolean isOwn) {
			if (!isOwn || !(item instanceof TextMessage textMessage)) {
				return null;
			}

			MenuItem editItem = new MenuItem("Bearbeiten");
			editItem.setOnAction(event -> startEditing(textMessage));
			return new ContextMenu(editItem);
		}
	}
}
