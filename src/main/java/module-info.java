module com.chatproject {
	requires javafx.fxml;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.base;
	requires org.hibernate.orm.core;
	requires jakarta.transaction;
	requires jakarta.interceptor;
	requires java.sql;
	requires webcam.capture;
	uses java.sql.Driver;


	requires jbcrypt;

	requires java.net.http;
	requires jakarta.persistence;
	requires java.desktop;

	opens Client to javafx.fxml, javafx.graphics, javafx.base;
	opens DBUtil to javafx.fxml;
	opens User.Model to org.hibernate.orm.core, javafx.base, jakarta.persistence;

	exports DBUtil;
	exports VideoCall;
	exports Server;
}
