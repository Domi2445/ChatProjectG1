module com.chatproject {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.net.http;

    opens Client to javafx.graphics, javafx.base;
    exports Client;
    exports Server;
    exports Util;
    exports Util.Emoji;
    exports Util.Message;
}