module com.chatproject {
	requires javafx.fxml;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.base;

	requires jbcrypt;

	requires java.net.http;
	requires jakarta.persistence;

	opens Client to javafx.fxml, javafx.graphics, javafx.base;
}
