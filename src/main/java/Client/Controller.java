package Client;

import Util.Network.Messages.FileMessage;
import Util.Network.Messages.Message;
import Util.Network.Messages.TextMessage;
import Util.Network.Notifications.JoinNotification;
import Util.Network.Notifications.LeaveNotification;
import Util.Network.Notifications.Notification;
import Util.Network.Packet;
import Util.User;
import VideoCall.AudioCall;
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

public class Controller {

    public static final int MAX_FILE_SIZE = 1_000_000;

    private final BlockingQueue<Packet> outPacketQueue;
    private final BlockingQueue<Packet> inPacketQueue;

    private Client client;
    private User localUser;
    private Stage stage;

    @FXML private ListView<Packet> messageListView;
    @FXML private TextField messageTextField;
    @FXML private Button sendButton;
    @FXML private Button uploadButton;
    @FXML private Button videoCallButton;

    // 🔊 AudioCall Feature
    private AudioCall audioCall = new AudioCall();
    private boolean inCall = false;

    private static final String RELAY_IP = "127.0.0.1";
    private static final int RELAY_PORT = 9000;
    private static final int MY_PORT = 7000;

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

                            case Notification notification -> Platform.runLater(() -> {
                                getMessages().add(notification);
                                messageListView.scrollTo(getMessages().size() - 1);
                                handleNotification(notification);
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
            Platform.runLater(() ->
                    getMessages().add(
                            new TextMessage(new User("System"),
                                    "Verbindung fehlgeschlagen: " + e.getMessage()))
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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            messageListView.scrollTo(getMessages().size() - 1);
            messageTextField.clear();
        }
    }

    private void sendFile() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null || !selectedFile.isFile()) return;

        if (selectedFile.length() > MAX_FILE_SIZE) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Die Datei ist zu groß!");
            alert.setContentText(selectedFile.length() + " / " + MAX_FILE_SIZE);
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

        FileMessage.FileType type =
                selectedFile.getName().matches(".*\\.(png|jpg|gif|bmp)")
                        ? FileMessage.FileType.IMAGE
                        : FileMessage.FileType.FILE;

        Message message = new FileMessage(localUser, bytes, selectedFile.getName(), type);

        try {
            outPacketQueue.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        messageListView.scrollTo(getMessages().size() - 1);
        messageTextField.clear();
    }

    // 🔊 Call Button Logic
    private void handleCallButton() {
        if (!inCall) {
            try {
                audioCall.start(RELAY_IP, RELAY_PORT, MY_PORT);
                inCall = true;
            } catch (Exception ex) {
                getMessages().add(
                        new TextMessage(new User("System"),
                                "Call failed: " + ex.getMessage())
                );
            }
        } else {
            audioCall.stop();
            audioCall = new AudioCall();
            inCall = false;
        }
    }

    private void handleNotification(Notification notification) {
        switch (notification) {
            case JoinNotification join ->
                    System.out.println(join.getUser() + " ist beigetreten");

            case LeaveNotification leave ->
                    System.out.println(leave.getUser() + " hat verlassen");

            case null, default ->
                    throw new IllegalStateException("Unbekannte Systemnachricht");
        }
    }

    // 💬 UI Rendering
    private class MessageCell extends ListCell<Packet> {

        @Override
        protected void updateItem(Packet item, boolean empty) {
            super.updateItem(item, empty);

            setText(null);
            setGraphic(null);

            if (empty || item == null) return;

			switch (item) {
				case Message message -> setGraphic(renderMessage(message));
				case Notification notification -> setText(notification.toString());
				case null, default -> {}
			}
        }

        private HBox renderMessage(Message message) {
            Label label = new Label(message.toString());
            label.setWrapText(true);

            boolean isOwn = localUser != null &&
                    message.getSender().getUsername().equals(localUser.getUsername());

            label.setStyle(isOwn
                    ? "-fx-background-color: lightblue; -fx-padding: 8;"
                    : "-fx-background-color: lightgray; -fx-padding: 8;");

            HBox box = new HBox(label);
            box.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            box.setPadding(new Insets(5));

            return box;
        }
    }
}
