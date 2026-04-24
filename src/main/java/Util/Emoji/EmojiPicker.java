package Util.Emoji;

import Util.Network.Messages.EmojiMessage;
import Util.Network.Messages.Message;
import User.Model.User;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.util.List;

public class EmojiPicker {
    private final User user;
	private final ObservableList<Message> messages;

    public EmojiPicker(User user, ObservableList<Message> messages) {
        this.user = user;
        this.messages = messages;
    }

    public void open() {
        Stage stage = new Stage();
        stage.setTitle("Emoji auswählen");

        FlowPane root = new FlowPane();
        root.setHgap(10);
        root.setVgap(10);

        List<Emoji> emojis = new EmojiService().loadEmojis();

        for (Emoji emoji : emojis) {
            root.getChildren().add(createEmojiButton(emoji, stage));
        }

        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.show();
    }

    private Button createEmojiButton(Emoji emoji, Stage stage) {
        Button btn = new Button(emoji.getCharacter());
        btn.setStyle("-fx-font-size: 20;");

        btn.setOnAction(e -> {
            messages.add(new EmojiMessage(user, emoji.getCharacter()));
            stage.close();
        });

        return btn;
    }
}
