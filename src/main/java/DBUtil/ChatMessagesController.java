package DBUtil;

import User.Model.ChatMessage;
import User.Repository.ChatMessageRepository;
import User.Repository.RepositoryException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ChatMessagesController implements Initializable {

    @FXML private TableView<ChatMessage> messagesTable;
    @FXML private TableColumn<ChatMessage, String> senderColumn;
    @FXML private TableColumn<ChatMessage, String> receiverColumn;
    @FXML private TableColumn<ChatMessage, String> chatRoomIdColumn;
    @FXML private TableColumn<ChatMessage, String> contentColumn;
    @FXML private TableColumn<ChatMessage, String> messageTypeColumn;
    @FXML private TableColumn<ChatMessage, String> timestampColumn;

    private final ChatMessageRepository repository = new ChatMessageRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        senderColumn.setCellValueFactory(new PropertyValueFactory<>("sender"));
        receiverColumn.setCellValueFactory(new PropertyValueFactory<>("receiver"));
        chatRoomIdColumn.setCellValueFactory(new PropertyValueFactory<>("chatRoomId"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        messageTypeColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getMessageType() != null ? cellData.getValue().getMessageType().toString() : ""));
        timestampColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTimestamp() != null
                    ? cellData.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : ""));

        loadMessages();
    }

    private void loadMessages() {
        try {
            List<ChatMessage> messages = repository.getAllMessages();
            ObservableList<ChatMessage> messageList = FXCollections.observableArrayList(messages);
            messagesTable.setItems(messageList);
        } catch (RepositoryException e) {
            System.err.println("Fehler beim Laden der Nachrichten: " + e.getMessage());
        }
    }

    @FXML
    private void refreshMessages() {
        loadMessages();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) messagesTable.getScene().getWindow();
        stage.close();
    }
}
