module com.chatproject {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.base;
	requires jbcrypt;
	requires javafx.fxml;
	requires java.desktop;
	requires jakarta.persistence;
	requires java.net.http;
	requires jakarta.transaction;
	requires jakarta.cdi;
	requires org.hibernate.orm.core;
	requires java.naming;

	opens Client to javafx.fxml, javafx.graphics, javafx.base;
    opens User.Model;
    exports User.Model;
    exports Client;
    exports Server;
    exports Util;
exports Util.Login;
	exports Util.Network;
	exports Util.Network.Messages;
	exports Util.Network.Notifications;
}
