module com.chatproject {
	requires javafx.fxml;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.base;
	requires org.hibernate.orm.core;
	requires jakarta.transaction;
	requires java.sql;


	requires jbcrypt;

	requires java.net.http;
	requires jakarta.persistence;

	opens Client to javafx.fxml, javafx.graphics, javafx.base;
	opens DBUtil to javafx.fxml;
	opens User.Model to org.hibernate.orm.core, javafx.base, jakarta.persistence;


	exports DBUtil;
}
