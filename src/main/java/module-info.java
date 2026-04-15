module com.chatproject {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;

    opens Client to javafx.graphics, javafx.base;
    exports Client;
    exports Server;
    exports Util;
    exports User.Login;
}
