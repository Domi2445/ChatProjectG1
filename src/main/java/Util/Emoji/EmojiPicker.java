package Util.Emoji;

import Client.View;
import Util.User;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.util.List;

public class EmojiPicker {

    public static void open(User user, View view) {

        Stage stage = new Stage();
        stage.setTitle("Emoji auswählen");

        FlowPane root = new FlowPane();
        root.setHgap(10);
        root.setVgap(10);

        List<Emoji> emojis = EmojiService.loadEmojis();

        for (Emoji emoji : emojis) {
            Button btn = new Button(emoji.getCharacter());
            btn.setStyle("-fx-font-size: 20;");

            btn.setOnAction(e -> {
                view.getMessages().add(new EmojiMessage(user, emoji.getCharacter()));
                stage.close();
            });

            root.getChildren().add(btn);
        }

        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.show();
    }
}