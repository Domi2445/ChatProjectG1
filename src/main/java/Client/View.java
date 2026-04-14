package Client;

import Util.FileMessage;
import Util.Message;
import Util.TextMessage;
import Util.User;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;

public class View {
	private final ListView<Message> messageListView;
	private final TextField messageTextField;
	private final Button sendButton;
	private final Button uploadButton;
	private final Button videoCallButton;

	// Verwendet für FileChooser in Controller.java
	private final Stage stage;
	private final User localUser;

	public View(Stage stage, User user) {
		this.stage = stage;
		this.localUser = user;

		stage.setTitle("Socket Chat");

		// --- Message area ---
		messageListView = new ListView<Message>();
		messageListView.setCellFactory(lv -> new MessageCell());
		messageListView.setStyle("-fx-background-color: #1e1e2e; -fx-control-inner-background: #1e1e2e;");
		VBox.setVgrow(messageListView, Priority.ALWAYS);

		// --- Input field ---
		messageTextField = new TextField();
		messageTextField.setPromptText("Nachricht eingeben...");
		messageTextField.setStyle(
				"-fx-background-color: #2a2a3d; -fx-text-fill: #cdd6f4; -fx-prompt-text-fill: #6c7086; "
				+ "-fx-border-color: #45475a; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 14;"
		);
		messageTextField.setFont(Font.font(14));
		HBox.setHgrow(messageTextField, Priority.ALWAYS);

		// --- Send button ---
		sendButton = new Button("\u27A4");
		sendButton.setStyle(
				"-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-size: 16; "
				+ "-fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"
		);
		sendButton.setTooltip(new Tooltip("Senden"));

		// --- Upload button ---
		uploadButton = new Button("\uD83D\uDCCE");
		uploadButton.setStyle(
				"-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 16; "
				+ "-fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"
		);
		uploadButton.setTooltip(new Tooltip("Datei hochladen"));

		// --- Video call button ---
		videoCallButton = new Button("\uD83D\uDCF9");
		videoCallButton.setStyle(
				"-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 16; "
				+ "-fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"
		);
		videoCallButton.setTooltip(new Tooltip("Videochat / Anruf"));

		// --- Top bar ---
		Label titleLabel = new Label("Socket Chat");
		titleLabel.setFont(Font.font("System", 18));
		titleLabel.setTextFill(Color.web("#cdd6f4"));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox topBar = new HBox(10, titleLabel, spacer, videoCallButton);
		topBar.setAlignment(Pos.CENTER_LEFT);
		topBar.setPadding(new Insets(10, 14, 10, 14));
		topBar.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 0 0 1 0;");

		// --- Input bar ---
		HBox inputBar = new HBox(8, uploadButton, messageTextField, sendButton);
		inputBar.setAlignment(Pos.CENTER);
		inputBar.setPadding(new Insets(10, 14, 10, 14));
		inputBar.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 1 0 0 0;");

		// --- Root layout ---
		VBox root = new VBox(topBar, messageListView, inputBar);
		root.setStyle("-fx-background-color: #1e1e2e;");

		Scene scene = new Scene(root, 500, 650);
		stage.setScene(scene);
		stage.setMinWidth(350);
		stage.setMinHeight(400);
		stage.show();
	}

	public Stage getStage() { return stage; }
	public ListView<Message> getMessageListView() { return messageListView; }
	public ObservableList<Message> getMessages() { return messageListView.getItems(); }
	public TextField getMessageTextField() { return messageTextField; }
	public Button getSendButton() { return sendButton; }
	public Button getUploadButton() { return uploadButton; }
	public Button getVideoCallButton() { return videoCallButton; }

	private class MessageCell extends ListCell<Message> {
		@Override
		protected void updateItem(Message item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setGraphic(null);
				setStyle("-fx-background-color: transparent;");
				return;
			}

			Node node;

			switch (item) {
				case TextMessage textMessage -> {
					Label label = new Label(textMessage.getContent());
					label.setWrapText(true);
					label.setMaxWidth(300);
					label.setFont(Font.font(13));
					node = label;
				}
				case FileMessage fileMessage -> {
					FileMessage.FileType fileType = fileMessage.getFileType();
					switch (fileType) {
						case FILE -> {
							Label label = new Label("Datei: " + fileMessage.getFileName());
							// todo: download
							node = label;
						}
						case IMAGE -> {
							Image image = new Image(new ByteArrayInputStream(fileMessage.getContent()));
							ImageView imageView = new ImageView(image);
							imageView.setPreserveRatio(true);
							imageView.fitWidthProperty().bind(Bindings.createDoubleBinding(
								() -> Math.min(Math.max(getScene().getWidth() - 32, 100.0), image.getWidth()),
								getScene().widthProperty()
							));
							node = imageView;
						}
						default -> throw new IllegalStateException("Unbekannter Dateityp: " + fileType);
					}
				}
				default -> throw new IllegalStateException("Unexpected value: " + item);
			}

			boolean isOwn = item.getSender().getIdentifier().equals(localUser.getIdentifier());

			if (isOwn) {
				node.setStyle(
					"-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; "
					+ "-fx-padding: 8 12; -fx-background-radius: 14 14 4 14;"
				);
			} else {
				node.setStyle(
					"-fx-background-color: #313244; -fx-text-fill: #cdd6f4; "
					+ "-fx-padding: 8 12; -fx-background-radius: 14 14 14 4;"
				);
			}

			HBox container = new HBox(node);
			container.setPadding(new Insets(2, 10, 2, 10));
			container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

			setGraphic(container);
			setStyle("-fx-background-color: transparent; -fx-padding: 0;");
		}
	}
}
