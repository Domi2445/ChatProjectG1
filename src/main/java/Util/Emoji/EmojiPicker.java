package Util.Emoji;

import Client.Controller;
import Util.User;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.util.List;

public class EmojiPicker {

    private final User user;
    private final Controller controller;

    public EmojiPicker(User user, Controller controller) {
        this.user = user;
        this.controller = controller;
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
        final Emoji finalEmoji = emoji;
        Button btn = new Button(emoji.getCharacter());
        btn.setStyle("-fx-font-size: 20;");

        btn.setOnAction(e -> {
            controller.sendEmojiMessage(user, finalEmoji.getCharacter());
            stage.close();
        });

        return btn;
    }
}