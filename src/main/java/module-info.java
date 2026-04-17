module com.chatproject {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
	requires javafx.fxml;
	requires java.desktop;
	requires jakarta.persistence;
    requires java.net.http;
    requires jakarta.transaction;
    requires jakarta.cdi;
    requires org.hibernate.orm.core;

    opens Client to javafx.fxml, javafx.graphics, javafx.base;
    opens User.Model;
    exports Client;
    exports Server;
    exports Util;
}