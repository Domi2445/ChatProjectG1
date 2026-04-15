module com.chatproject {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
	requires javafx.fxml;
	requires java.desktop;
    requires java.net.http;

    opens Client to javafx.fxml, javafx.graphics, javafx.base;
    exports Client;
    exports Server;
    exports Util;
}