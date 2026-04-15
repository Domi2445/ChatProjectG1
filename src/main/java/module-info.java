module com.chatproject {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
	requires javafx.fxml;
	requires java.desktop;
	requires jakarta.persistence;

	opens Client to javafx.fxml, javafx.graphics, javafx.base;
    opens User.Model to jakarta.persistence;
    exports Client;
    exports Server;
    exports Util;
}